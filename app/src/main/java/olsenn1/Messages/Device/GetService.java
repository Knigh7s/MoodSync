package olsenn1.Messages.Device;

import olsenn1.Messages.DataTypes.Payload;

public class GetService extends Payload {
	int code = 2;
	
	public GetService() {}
	
	public int getCode() {
		return code;
	}
}
