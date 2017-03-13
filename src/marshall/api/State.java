package marshall.api;

interface State {
	void doAction(MarshallMessage message);
}