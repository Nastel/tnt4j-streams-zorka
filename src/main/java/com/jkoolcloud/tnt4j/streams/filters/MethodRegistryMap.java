/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * This file is part of TNT4J-Streams-Zorka.
 *
 * TNT4J-Streams-Zorka is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TNT4J-Streams-Zorka is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TNT4J-Streams-Zorka.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jkoolcloud.tnt4j.streams.filters;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The type Method registry map. Class needed for JAXB work properly.
 *
 * @version $Revision: 1 $
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MethodRegistryMap {

	private Map<Integer, MethodRegistry> methodRegistryMap = new ConcurrentHashMap<Integer, MethodRegistry>();

	/**
	 * Constructs a new MethodRegistryMap.
	 */
	public MethodRegistryMap() {
		super();
	}

	/**
	 * Get method registry.
	 *
	 * @param key
	 *            the key of map
	 * @return the method registry
	 */
	public MethodRegistry get(Integer key) {
		return methodRegistryMap.get(key);
	}

	/**
	 * Entry set set of method registry.
	 *
	 * @return the set
	 */
	public synchronized Set<Entry<Integer, MethodRegistry>> entrySet() {
		return methodRegistryMap.entrySet();
	}

	/**
	 * Put method registry.
	 *
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 * @return the method registry
	 */
	public MethodRegistry put(Integer key, MethodRegistry value) {
		return methodRegistryMap.put(key, value);
	}

	/**
	 * Lookup method registry.
	 *
	 * @param symbolToLookFor
	 *            the symbol to look for
	 * @param signatureToLookFor
	 *            the signature to look for
	 * @return the method registry
	 */
	public MethodRegistry lookup(String symbolToLookFor, String signatureToLookFor) {
		for (Entry<Integer, MethodRegistry> entry : methodRegistryMap.entrySet()) {
			MethodRegistry value = entry.getValue();
			if (value != null) {
				String symbolName = value.getSymbolName();
				String signature = value.getSymbolSignature();
				if (symbolName != null && symbolName.equals(symbolToLookFor) && signature.equals(signatureToLookFor)) {
					return value;
				}
			}
		}
		return null;
	}

	/**
	 * Fixes symbol registry if method name and ID does not match.
	 *
	 * @param newSymbol
	 *            the new symbol
	 * @param methodRegistry
	 *            the method registry
	 *
	 * @see MethodRegistry
	 */
	public synchronized void fixup(Integer newSymbol, MethodRegistry methodRegistry) {
		int oldSymbol = methodRegistry.getSymbol();
		methodRegistry.setSymbol(newSymbol);
		methodRegistryMap.put(methodRegistry.getSymbol(), methodRegistryMap.remove(oldSymbol));
	}

}
