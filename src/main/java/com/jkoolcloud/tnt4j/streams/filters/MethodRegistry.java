/*
 * Copyright 2014-2017 JKOOL, LLC.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Method registry for saving method times and method context.
 *
 * @version $Revision: 1 $
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MethodRegistry implements Serializable {
	private static final long serialVersionUID = -8007068054070054472L;

	private static final int ARRAY_SIZE_FACTOR = 2;

	@XmlElement(name = "times", type = Long.class)
	private List<Number> times;
	private Double upperBB;
	private Double lowerBB;
	private transient boolean recalcNeeded = false;
	private transient boolean symbolRegistryCheckNeeded = true;
	private int symbol;
	private String symbolName;
	private String symbolSignature;
	private static transient int arraySize;

	/**
	 * Constructs a new Method registry. JAXB needed default constructor
	 */
	public MethodRegistry() {
	}

	/**
	 * Constructs a new Method registry. Default constructor.
	 *
	 * @param symbolName
	 *            the symbol name, name of method
	 * @param symbolSignature
	 *            the symbol signature, signature of given method
	 * @param symbolNr
	 *            the symbol identifier
	 * @param nPeriod
	 *            the N-period for determine max array size
	 */
	public MethodRegistry(String symbolName, String symbolSignature, int symbolNr, int nPeriod) {
		this.symbolSignature = symbolSignature;
		this.symbolName = symbolName;
		this.symbol = symbolNr;
		MethodRegistry.arraySize = nPeriod * ARRAY_SIZE_FACTOR;
		this.times = new ArrayList<>(arraySize);
	}

	/**
	 * Is recalculation of Bollinger bands needed.
	 *
	 * @return {@code true} if recalculation make sense
	 */
	public boolean isRecalcNeeded() {
		return recalcNeeded;
	}

	/**
	 * Gets upper Bollinger bands value.
	 *
	 * @return Bollinger bands value
	 */
	public double getUpperBB() {
		return upperBB;
	}

	/**
	 * Sets upper Bollinger bands value.
	 *
	 * @param upperBB
	 *            upper Bollinger bands value
	 */
	public void setUpperBB(double upperBB) {
		this.upperBB = upperBB;
		recalcNeeded = false;
	}

	/**
	 * Gets lower Bollinger bands value.
	 *
	 * @return the Bollinger bands value
	 */
	public double getLowerBB() {
		return lowerBB;
	}

	/**
	 * Sets lower Bollinger bands value.
	 *
	 * @param lowerBB
	 *            lower Bollinger bands value
	 */
	public void setLowerBB(double lowerBB) {
		this.lowerBB = lowerBB;
		recalcNeeded = false;
	}

	/**
	 * Add time to method registry.
	 *
	 * @param element
	 *            the method time
	 * @return {@code true} if added successfully
	 */
	public boolean add(Number element) {
		if (upperBB == null || lowerBB == null) {
			upperBB = element.doubleValue();
			lowerBB = element.doubleValue();
		}
		recalcNeeded = true;
		if (arraySize <= times.size()) {
			times.remove(0);
		}
		return times.add(element);
	}

	/**
	 * Check symbol and method signature matches registry.
	 *
	 * @param id
	 *            the symbol id
	 * @param name
	 *            the name of method
	 * @return {@code true} if symbol matches the name
	 */
	public boolean checkSymbolName(int id, String name) {
		if (id == symbol && name.equals(symbolName)) {
			symbolRegistryCheckNeeded = false;
			return true;
		}
		return false;
	}

	/**
	 * Is need to check symbol registry to match method signature and ID. Check is performed after always after restore
	 * from persistence.
	 *
	 * @return {@code true} if check is needed
	 */
	public boolean isSymbolRegistryCheckNeeded() {
		return symbolRegistryCheckNeeded;
	}

	/**
	 * Gets symbol - id of method.
	 *
	 * @return the symbol
	 */
	public int getSymbol() {
		return symbol;
	}

	/**
	 * Sets the symbol.
	 *
	 * @param symbol
	 *            the symbol
	 */
	public void setSymbol(int symbol) {
		this.symbol = symbol;
	}

	/**
	 * Gets symbol name - method name.
	 *
	 * @return the symbol name
	 */
	public String getSymbolName() {
		return symbolName;
	}

	/**
	 * Gets times of method saved.
	 *
	 * @return the times
	 */
	public List<Number> getTimes() {
		return times;
	}

	/**
	 * Gets symbol signature.
	 *
	 * @return the symbol signature
	 */
	public String getSymbolSignature() {
		return symbolSignature;
	}
}
