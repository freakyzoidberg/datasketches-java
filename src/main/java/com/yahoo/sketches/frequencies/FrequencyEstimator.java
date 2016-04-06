/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */
package com.yahoo.sketches.frequencies;

/**
 * @author Edo Liberty
 * @author Justin Thaler
 */

/**
 * Abstract class for a FrequencyEstimator algorithm. Supports the ability to process a data stream
 * of (item, increment) pairs, where item is an identifier, specified as a long, and increment is a
 * non-negative integer that is also specified as a long. The frequency of an identifier is defined
 * to be the sum of associated the increments. Any FrequencyEstimator algorithm must be able to: 1)
 * estimate the frequency of an identifier, 2) return upper and lower bounds on the frequency
 * (depending on the implementation, these bounds may hold deterministically, or with high
 * probability), 3) return an upper bound on the maximum error in any estimate (which also might
 * hold deterministically or with high probability, depending on implementation) 4) Return an array
 * of keys whose frequencies might be above a certain threshold (specifically, the threshold is
 * 1/errorTolerance + 1) 5) merge itself with another FrequencyEstimator algorithm from the same
 * instantiation of the abstract class.
 * 
 */
public abstract class FrequencyEstimator {

  /**
   * Update this sketch with a key and a frequency count of one.
   * @param key for which the frequency should be increased. 
   */
  public abstract void update(long key);

  /**
   * Update this sketch with a key and a positive frequency count. 
   * @param key for which the frequency should be increased. The key can be any long value and is 
   * only used by the sketch to determine uniqueness.
   * @param increment the amount by which the frequency of the key should be increased. 
   * An increment of zero is a no-op, and a negative value will throw an exception.
   */
  public abstract void update(long key, long increment);

  /**
   * Gets the estimate of the frequency of the given key. 
   * Note: The true frequency of a key would be the sum of the counts as a result of the two 
   * update functions.
   * @param key the given key
   * @return the estimate of the frequency of the given key
   */
  public abstract long getEstimate(long key);

  /**
   * Gets the guaranteed lower bound frequency of the given key.
   * @param key the given key.
   * @return the guaranteed lower bound frequency of the given key. That is, a number which is 
   * guaranteed to be no larger than the real frequency.
   */
  public abstract long getLowerBound(long key);

  /**
   * Gets the guaranteed upper bound frequency of the given key.
   * @param key the given key
   * @return the guaranteed upper bound frequency of the given key. That is, a number which is 
   * guaranteed to be no smaller than the real frequency.
   */
  public abstract long getUpperBound(long key);

  /**
   * @return An upper bound on the maximum error of getEstimate(key) for any key. 
   * This is equivalent to the maximum distance between the upper bound and the lower bound for 
   * any key.
   */
  public abstract long getMaxError();


  /**
   * Gets a superset of all keys with estimated frequency above the given threshold. 
   * There may be keys in the set with true frequencies less than the threshold (false positives).
   * There will not be any keys with true frequencies greater than the threshold that are not 
   * members of the set (false positives).
   * @param threshold the given frequency threshold
   * @return an array of keys containing a superset of all keys whose true frequencies are are 
   * least the error tolerance.
   */
  public abstract long[] getFrequentKeys(long threshold);

  /**
   * This function merges two FrequencyEstimator sketches, potentially of different sizes.
   * 
   * @param other another FrequenciesEstimator of the same class
   * @return a pointer to a FrequencyEstimator whose estimates are within the guarantees of the
   * largest error tolerance of the two merged sketches. This method does not create a new
   * sketch. The sketch whose function is executed is changed and a reference to it is
   * returned.
   */
  public abstract FrequencyEstimator merge(FrequencyEstimator other);

  /**
   * Returns the current number of counters the sketch is configured to support.
   * 
   * @return the current number of counters the sketch is configured to support.
   */
  public abstract int getK();

  /**
   * Returns the maximum number of counters the sketch will ever be configured to support.
   * 
   * @return the maximum number of counters the sketch will ever be configured to support.
   */
  public abstract int getMaxK();

  /**
   * Returns true if this sketch is empty
   * 
   * @return true if this sketch is empty
   */
  public abstract boolean isEmpty();

  /**
   * Returns the sum of the frequencies in the stream seen so far by the sketch
   * 
   * @return the sum of the frequencies in the stream seen so far by the sketch
   */
  public abstract long getStreamLength();

  /**
   * Resets this sketch to a virgin state, but retains the original value of the error parameter
   */
  public abstract void reset();

}
