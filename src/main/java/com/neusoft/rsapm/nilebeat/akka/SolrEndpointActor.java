package com.neusoft.rsapm.nilebeat.akka;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.neusoft.rsapm.nilebeat.akka.dto.EndPointFailed;
import com.neusoft.rsapm.nilebeat.akka.dto.NewLineEvent;
import com.neusoft.rsapm.nilebeat.configuration.SolrEndPointConfiuration;
import com.neusoft.rsapm.nilebeat.guice.GuiceAbstractActor;
import com.neusoft.rsapm.nilebeat.retry.RetryCommand;
import com.neusoft.rsapm.nilebeat.util.solr.SolrWriter;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

/**  
    * @ClassName: SolrEndpointActor  
    * @Description: push solr Actor 
    * @author Muxin Sun
    * @date 2018-05-22  
    *    
*/  
@Slf4j
public class SolrEndpointActor extends GuiceAbstractActor {
	

	private final static String SCHEDULATION_CHECK = "SchedulationsCheck";

	private Cancellable schedule;
	private final ActorSystem system;
	private final SolrWriter sw;
	private final SolrEndPointConfiuration conf;
	
	@Inject
	public SolrEndpointActor(SolrEndPointConfiuration conf) {				                
		String solr_url = String.format("%s/%s", conf.getUrl(), conf.getCore());
		
        this.sw = new SolrWriter(solr_url, conf.getUsername(), conf.getPassword());
        this.conf = conf;
        
        this.system = getContext().getSystem();
        
		this.schedule = system.scheduler().scheduleOnce(
				FiniteDuration.create(1, TimeUnit.SECONDS), 
				getSelf(), SCHEDULATION_CHECK, 
				system.dispatcher(), getSelf());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.matchEquals(SCHEDULATION_CHECK, sc -> {	
				
				LOGGER.info("### Solr Flush");
				
				sw.flush();
				
				this.schedule = system.scheduler().scheduleOnce(
						FiniteDuration.create(5, TimeUnit.SECONDS), 
						getSelf(), SCHEDULATION_CHECK, 
						system.dispatcher(), getSelf());

			})
			.match(JsonObject.class, j -> send(j))
			.match(NewLineEvent.class, s -> send(s))
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
				unhandled(o);
			})
			.build();
	}
	
	@Override
	public void postStop() throws Exception {
		super.postStop();
		LOGGER.info("end {} ", getSelf().path());
		
		schedule.cancel();
		sw.close();
		getContext().parent().tell(new EndPointFailed(conf), ActorRef.noSender());
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
	}
	
	private void send(NewLineEvent s) {
		new RetryCommand(3, s.getPath()).run(new Callable<Void>() {						
			@Override
			public Void call() throws Exception {
				sw.write(new JsonParser().parse(new Gson().toJson(s)).getAsJsonObject());
				LOGGER.info("send {}", s.toString());
				return null;
			}		
		});										
	}
	
	private void send(JsonObject j) {
		new RetryCommand(3, "json data").run(new Callable<Void>() {						
			@Override
			public Void call() throws Exception {
				sw.write(j);
				LOGGER.info("send {}", j.toString());
				return null;
			}		
		});										
	}
}	
