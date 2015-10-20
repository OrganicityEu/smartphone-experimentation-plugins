package eu.smartsantander.androidExperimentation.jsonEntities;

public class Reading {

    private String context;
    private String value;
    private long timestamp;

    public Reading(final String val, final String context) {
        this.value = val;
        this.context = context;
        this.timestamp = System.currentTimeMillis();
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
