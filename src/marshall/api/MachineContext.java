package marshall.api;

public class MachineContext implements State {

	private  State machineState;
		
	public void setState(State state) {
		this.machineState = state;
	}

	public State getState() {
		return this.machineState;
	}

	//public void reset() {
	// this.machineState = null;
	//}

		
	@Override
	public void doAction(MarshallMessage message){
		this.machineState.doAction(message);	
	}
}