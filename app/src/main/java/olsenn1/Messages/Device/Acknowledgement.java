package olsenn1.Messages.Device;

import olsenn1.Messages.DataTypes.Payload;

public class Acknowledgement extends Payload{
	int code = 45;
	
	public Acknowledgement() {}
	
	public int getCode() {
		return code;
	}
}
