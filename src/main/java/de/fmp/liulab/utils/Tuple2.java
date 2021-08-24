package de.fmp.liulab.utils;

/**
 * Class responsible for creating tuple
 * @author diogobor
 *
 * @param <K> first param
 * @param <V> second param
 */
public class Tuple2 {
	 
    private Object first;
    private Object second;
  
    public Tuple2(Object first, Object second){
        this.first = first;
        this.second = second;
    }
 
    public Object getFirst() {
    	return first;
    }
    
    public Object getSecond() {
    	return second;
    }
}