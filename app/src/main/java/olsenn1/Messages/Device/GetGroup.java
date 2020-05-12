package olsenn1.Messages.Device;

import olsenn1.Messages.DataTypes.Payload;

public class GetGroup extends Payload{
	int code = 51;
	
	public GetGroup() {}
	
	public int getCode() {
		return code;
	}

}
