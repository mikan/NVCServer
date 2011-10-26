package jp.ac.jaist.skdlab.nvcsys;

import java.util.EventObject;

public class MessageEvent extends EventObject {

	/** Default UID */
	private static final long serialVersionUID = 1L;
	
	private NVCClientUser source;
	private String name;
	private String value;
	
	public MessageEvent(NVCClientUser source, String name, String value) {
		super(source);
		this.source = source;
		this.name = name;
		this.value = value;
	}
	
	public NVCClientUser getUser() {
		return source;
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
}
