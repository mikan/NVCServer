package jp.ac.jaist.skdlab.nvcsys;

import java.util.EventListener;

public interface MessageListener extends EventListener {

	void messageThrow(MessageEvent e);
}
