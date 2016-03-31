package edu.sjtu.cs.action.util;

/**
 * Created by xinyu on 16-3-30.
 */
public class Pair {private String dep;
    private int position;
    public Pair(String dep, int position){
        this.dep = dep;
        this.position = position;
    }

    public String getDep(){
        return this.dep;
    }

    public int getPos(){
        return this.position;
    }

    public String toString(){
        return dep + "_" + position;
    }
}
