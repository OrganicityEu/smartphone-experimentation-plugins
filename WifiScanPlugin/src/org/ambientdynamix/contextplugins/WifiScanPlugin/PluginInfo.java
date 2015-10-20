package org.ambientdynamix.contextplugins.WifiScanPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.ambientdynamix.api.application.IContextInfo;
import com.google.gson.Gson;
import eu.smartsantander.androidExperimentation.jsonEntities.Reading;
import android.os.Parcel;
import android.os.Parcelable;

class PluginInfo implements IContextInfo,IPluginInfo  {
	public static Parcelable.Creator<PluginInfo> CREATOR = new Parcelable.Creator<PluginInfo>() {
		public PluginInfo createFromParcel(Parcel in) {
			return new PluginInfo(in);
		}
		public PluginInfo[] newArray(int size) {
			return new PluginInfo[size];
		}
	};
	
	public static String CONTEXT_TYPE = "org.ambientdynamix.contextplugins.WifiScanPlugin";
	private String state;
	private String payload = "";
	private String context;
	
	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}
	
	public PluginInfo() {
		this.context=CONTEXT_TYPE;
	}

	@Override
	public Set<String> getStringRepresentationFormats() {
		Set<String> formats = new HashSet<String>();
		formats.add("text/plain");
		formats.add("dynamix/web");
		return formats;
	}

	@Override
	public String getStringRepresentation(String format)
	{
		if (format.equalsIgnoreCase("text/plain"))
		{	
				return CONTEXT_TYPE ;
		}
		else if (format.equalsIgnoreCase("dynamix/web"))
		{
			return CONTEXT_TYPE ;
		}
		else if (format.equalsIgnoreCase("json"))
		{
			return (new Gson()).toJson(this);
		}
		else
		{
			return (new Gson()).toJson(this);
		}
	}

	@Override
	public String getImplementingClassname() {
		return this.getClass().getName();
	}

	@Override
	public String getContextType() {
		return CONTEXT_TYPE;
	}

	public String getPayload()
	{
		return this.payload;
	}

	public void setPayload(List<Reading> r)
	{
		this.payload = (new Gson()).toJson(r);
	}
	
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	};

	public void writeToParcel(Parcel out, int flags)
	{
		out.writeString(this.payload);
		out.writeString(this.state);
	}

	private PluginInfo(final Parcel in)
	{
		this.payload = in.readString();
		this.state = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public String getState() {
		return this.state;
	}

	@Override
	public void setState(String state) {
		this.state=state;
		
	}
}