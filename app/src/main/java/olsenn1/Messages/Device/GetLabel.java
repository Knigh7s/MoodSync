package olsenn1.Messages.Device;

import olsenn1.Messages.DataTypes.Payload;

public class GetLabel extends Payload{
	int code = 23;
	
	public GetLabel() {}
	
	public int getCode() {
		return code;
	}
}
