package com.skillbill.at.service;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.skillbill.at.configuration.ConfigurationValidator.SingleConfiguration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileSystemWatcher implements AutoCloseable {	
	
	//do the same work with one map Please!!??
	private final Map<SingleConfiguration, WatchService> watchers;
	private final Map<SingleConfiguration, WatchKey> keys;

	@Inject
	public FileSystemWatcher() {
		this.watchers = new HashMap<>();
		this.keys = new HashMap<>();
	}

	@Override
	public void close() {
		watchers.values().forEach(ws -> {
			try {
				ws.close();
			} catch (IOException e) {
				LOGGER.error("", e);				
			}
		});		
	}

	public void watch(SingleConfiguration sc) {
		try {
			final Path path = sc.getPath().getParentFile().toPath();
			final WatchService wService = path.getFileSystem().newWatchService();
			
			keys.put(sc, path.register(wService, ENTRY_CREATE));
			watchers.put(sc, wService);			
		} catch (Exception e) {
			LOGGER.error("", e);
		}										
	}
	
	public Map<SingleConfiguration, WatchKey> getKeys() {
		return Collections.unmodifiableMap(keys);
	}	
}
