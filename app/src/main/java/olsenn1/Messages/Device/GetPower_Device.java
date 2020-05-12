package olsenn1.Messages.Device;

import olsenn1.Messages.DataTypes.Payload;

public class GetPower_Device extends Payload {
	int code = 20;
	
	public GetPower_Device() {}
	
	public int getCode() {
		return code;
	}
}
