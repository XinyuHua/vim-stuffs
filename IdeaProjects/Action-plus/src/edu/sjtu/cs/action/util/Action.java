package edu.sjtu.cs.action.util;

/**
 * Created by xinyu on 16-3-30.
 */
public class Action {private String verb;
    private String subject;
    private String object;

    public Action(String s, String v, String o){
        this.verb = v;
        this.subject = s;
        this.object = o;
    }

    public Action(String v){
        this.verb = v;
        this.subject = "Undecided";
        this.object = "Undecided";
    }

    public void setSubj(String s){
        this.subject = s;
    }

    public void setObj(String s){
        this.object = s;
    }

    public String getVerb(){
        return this.verb;
    }

    public String getSubj(){
        return this.subject;
    }

    public String getObj(){
        return this.object;
    }

    public String toString(){
        return this.subject + "_" + this.verb + "_" + this.object;
    }
}
