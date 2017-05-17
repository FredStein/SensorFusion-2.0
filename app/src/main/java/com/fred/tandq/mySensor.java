package com.fred.tandq;

import android.widget.TextView;

import java.util.HashMap;

/**
 * Created by Fred Stein on 16/05/2017.
 */

class mySensor {
    private String name;
    public String getName() {
        return name;
    }
    private int type;
    public int getType() {
        return type;
    }
    private String abbr;
    public String getAbbr() {
        return abbr;
    }
    private String[] dim;
    public void setDim(String[] dim){
        this.dim = new String[dim.length];
        this.dim = dim;
        setKeys(dim);
    }
    public String[] getDim() {
        return dim;
    }
    private HashMap<String, String> displayFieldIdx = new HashMap<>();
    public HashMap<String, String> getFieldIdx(){
        return this.displayFieldIdx;
    }
    private HashMap<String, TextView> displayFields = new HashMap<>();

    mySensor(String name, int type, String abbr){
        this.name = name;
        this.type = type;
        this.abbr = abbr;
    }
    private void setKeys(String[] dim){
        for (String item : dim){
            this.displayFieldIdx.put(abbr+item,item);
            this.displayFields.put(item,null);
        }
    }
    public void setTextField(String tvID, TextView tv){
        this.displayFields.put(displayFieldIdx.get(tvID),tv);
    }
    public TextView getTextView(String dim){
        return this.displayFields.get(dim);
    }
}
