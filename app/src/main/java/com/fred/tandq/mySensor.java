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
    private String[] mDim;
    public void setDim(String[] dim){
        mDim = new String[dim.length];
        mDim = dim;
        setKeys(dim);
    }
    public String[] getDim() {
        return mDim;
    }
    private HashMap<String, String> displayFieldIdx = new HashMap<>();
    public HashMap<String, String> getFieldIdx(){
        return displayFieldIdx;
    }

    mySensor(String name, int type, String abbr){
        this.name = name;
        this.type = type;
        this.abbr = abbr;
    }
    private void setKeys(String[] dim){
        for (String item : dim){
            displayFieldIdx.put(abbr+item,item);
            displayFields.put(item,null);
        }
    }
    private HashMap<String, TextView> displayFields = new HashMap<>();
    public void setTextField(String tvID, TextView tv){
        displayFields.put(displayFieldIdx.get(tvID),tv);
    }
    public TextView getTextView(String dim){
        return displayFields.get(dim);
    }
}
