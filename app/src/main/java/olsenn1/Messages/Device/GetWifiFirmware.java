package olsenn1.Messages.Device;

import olsenn1.Messages.DataTypes.Payload;

public class GetWifiFirmware extends Payload{
	int code = 18;
	
	public GetWifiFirmware() {}
	
	public int getCode() {
		return code;
	}
}
