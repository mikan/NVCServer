package jp.ac.jaist.skdlab.nvcsys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * NVCClient's session
 * 
 * @author Yutaka Kato
 * @version 0.0.1
 */
public class NVCClientUser implements Runnable, MessageListener {
	
	private int id;
	private Socket socket = null;
	private List<MessageListener> messageListenerList = null;
	private String name = null;
	private NVCServer server = NVCServer.getInstance();
	
	public NVCClientUser(Socket socket, int id) {
		this.socket = socket;
		this.id = id;
		messageListenerList = new ArrayList<MessageListener>();
		addMessageListener(this);
		
		System.out.println("[" + id + "] Accepted client from " + 
				socket.getInetAddress().getHostAddress());
		
		// Start client session
		Thread thread = new Thread(this);
		thread.start();
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void close() throws IOException {
		server.removeUser(this);
		messageListenerList.clear();
		socket.close();
		System.out.println("[" + id + "] Connection closed");
	}
	
	public void sendMessage(String message) {
		try {
			PrintWriter writer = new PrintWriter(socket.getOutputStream());
			writer.println(message);
			writer.flush();
		} catch (IOException e) {
			System.err.println("IO Error at sendMessage()");
		}
		System.out.println("[" + id + "] Sent message: " + message);
	}
	
	public void reachedMessage(String name, String value) {
		System.out.println("[" + id + "] Reached message: " + name + " " + value);
		MessageEvent event = new MessageEvent(this, name, value);
		
//		for (MessageListener l : messageListenerList) {
//			l.messageThrow(event);
//		}
		for (int i = 0; i < messageListenerList.size(); i++) {
			messageListenerList.get(i).messageThrow(event);
		}
	}
	
	public void addMessageListener(MessageListener l) {
		messageListenerList.add(l);
	}
	
	public void removeMessageListener(MessageListener l) {
		messageListenerList.remove(l);
	}
	
	public List<MessageListener> getMessageListenerList() {
		return messageListenerList;
	}

	@Override
	public void run() {
		try {
			// Get user requests
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			while (!socket.isClosed()) {
				String messageSource = reader.readLine();
				if (messageSource == null) {
					throw new NullPointerException("Couldn't get next message.");
				}
				String[] message = messageSource.split(" ", 2);
				String name = message[0];
				String value = message.length < 2 ? "" : message[1];
				reachedMessage(name, value);
			}
		} catch (IOException e) {
			System.err.println("[" + id + "] IO Error at NVCClientUser.run()");
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.err.println("[" + id + "] NULL Error at NVCClientUser.run()");
		}
	}

	@Override
	public void messageThrow(MessageEvent e) {
		
		String messageType = e.getName();
		String messageValue = e.getValue();
		
		// CLOSE: Disconnect
		if (messageType.equals("CLOSE")) {
			try {
				close();
			} catch (IOException ioe) {
				System.err.println("[" + id + "] IO Error at messageThrow()");
			}
		}
		
		// CHANGE: Change name
		else if (messageType.equals("CHANGE")) {
			String name = messageValue;
			if (name.indexOf(" ") == -1) {
				String before = getName();
				setName(name);
				sendMessage("OK");
				reachedMessage("MESSAGE", "User name changed: " + before +
						" to " + name);
			} else {
				sendMessage("ERROR Don't contain whitespace in name");
			}
		}
		
		// ADDD: Add a discussion
		else if (messageType.equals("ADDD")) {
			String title = messageValue;
			if (name.indexOf(" ") == -1) {
				Discussion discussion = new Discussion(title, this);
				discussion.addUser(this);			// Enter
				server.addDiscussion(discussion);	// Add	
				sendMessage("ADDD_R Succesful ADDD");
			} else {
				sendMessage("ERROR Failed to add discussion: " +
						"Don't insert whitespace in title");
			}
		}
		
		// GETD: Get current discussion list
		else if (messageType.equals("GETD")) {
			String result = "";
			List<Discussion> discussionList = server.getDiscussionList();
			for (int i = 0; i < discussionList.size(); i++) {
				result += discussionList.get(i).getTitle();
				if (i != discussionList.size() - 1) {
					result += ",";
				}
			}
			sendMessage("GETD_R " + result);
		}
		
		// ENTER: Enter the discussion
		else if (messageType.equals("ENTER")) {
			Discussion discussion = server.getDiscussion(messageValue);
			if (discussion != null) {
				discussion.addUser(this);
				sendMessage("ENTER_R Succesful ENTER");
			} else {
				sendMessage("ERROR Discussion not found: " + messageValue);
			}
		}
		
		// GETU: Get current discussing member list
		else if (messageType.equals("GETU")) {
			Discussion discussion = server.getDiscussion(messageValue);
			if (discussion != null) {
				String result = "";
				List<NVCClientUser> userList = discussion.getUserList();
				for (int i = 0; i < userList.size(); i++) {
					result += userList.get(i).getName();
					if (i != userList.size() - 1) {
						result += ",";
					}
				} 
				sendMessage("GETU_R " + result);
			}
		}
		
		// MESSAGE: Message
		else if (messageType.equals("MESSAGE")) {
			
		}
		
		// UP_ALL: Up screen brightness for all users
//		else if (messageType.equals("UP_ALL")) {
//			sendMessage("UP_ALL " + messageValue);
//		}
		
		// UP_ALL: Down screen brightness for all users
//		else if (messageType.equals("DOWN_ALL")) {
//			sendMessage("DOWN_ALL " + messageValue);
//		}
		
		// UP_ALL: Up screen brightness for all users
//		else if (messageType.equals("UP")) {
//			if (messageValue.equals(name)) {
//				sendMessage("UP " + name);
//			}
//		}
		
		// EXIT: Exit of discussion
		else if (messageType.equals("EXIT")) {
			Discussion discussion = server.getDiscussion(messageValue);
			if (discussion != null) {
				discussion.removeUser(this);
				sendMessage("MESSAGE exit success");
			} else {
				sendMessage("ERROR discussion not found: " + messageValue);
			}
		}
		
		// Others
		else {
			sendMessage("ERROR Unknown message type: " + messageType);
		}
	}

	@Override
	public String toString() {
		return "NAME=" + getName();
	}
	
}
