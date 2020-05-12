package olsenn1.Messages.Device;

import olsenn1.Messages.DataTypes.Payload;

public class GetLocation extends Payload{
	int code = 48;
	
	public GetLocation() {}
	
	public int getCode() {
		return code;
	}

}
