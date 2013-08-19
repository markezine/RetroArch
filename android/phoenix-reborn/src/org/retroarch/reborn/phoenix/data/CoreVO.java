package org.retroarch.reborn.phoenix.data;

public class CoreVO {
	public static final String TYPE_ITEM = "item";
	public static final String TYPE_TITLE = "title";
	public String type;
	public String id;
	public String corePackage;
	public String system;
	public String displayName;
	public Boolean installed;
	
	
	public CoreVO(String type, String id, String displayName, String system, String corePackage, Boolean installed){
		this.type = type;
		this.id = id;
		this.displayName = displayName;
		this.system = system;
		this.corePackage = corePackage;
		this.installed = installed;
	}
	
	public CoreVO(String type, String displayName){
		this(type, "", displayName, "", "", false);
	}
}
