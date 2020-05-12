package olsenn1.Messages.Device;

import olsenn1.Messages.DataTypes.Payload;

public class GetInfo extends Payload{
	int code = 34;
	
	public GetInfo() {}
	
	public int getCode() {
		return code;
	}

}
