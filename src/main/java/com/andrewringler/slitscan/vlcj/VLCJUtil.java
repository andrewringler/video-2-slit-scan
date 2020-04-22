package com.andrewringler.slitscan.vlcj;

import java.util.concurrent.atomic.AtomicBoolean;

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.factory.discovery.strategy.NativeDiscoveryStrategy;

public class VLCJUtil {
	public static boolean vlcInstallationFound() {
		AtomicBoolean loadedVLCNative = new AtomicBoolean(false);
		try {
			NativeDiscovery nd = new NativeDiscovery() {
				@Override
				protected void onFailed(String path, NativeDiscoveryStrategy strategy) {
					loadedVLCNative.set(false);
				}
				
				@Override
				protected void onNotFound() {
					loadedVLCNative.set(false);
				}
			};
			if (nd.discover()) {
				loadedVLCNative.set(true);
			}
		} catch (Error e) {
			loadedVLCNative.set(false);
		}
		return loadedVLCNative.get();
	}
}
