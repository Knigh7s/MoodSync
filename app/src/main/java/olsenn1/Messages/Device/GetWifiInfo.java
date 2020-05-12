package olsenn1.Messages.Device;

import olsenn1.Messages.DataTypes.Payload;

public class GetWifiInfo extends Payload {
	int code = 16;
	
	public GetWifiInfo() {}
	
	public int getCode() {
		return code;
	}

}
