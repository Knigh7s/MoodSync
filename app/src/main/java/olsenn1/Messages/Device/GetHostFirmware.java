package olsenn1.Messages.Device;

import olsenn1.Messages.DataTypes.Payload;

public class GetHostFirmware extends Payload{
	int code = 14;
	
	public GetHostFirmware() {}
	
	public int getCode() {
		return code;
	}
}
