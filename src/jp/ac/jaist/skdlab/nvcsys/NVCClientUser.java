package jp.ac.jaist.skdlab.nvcsys;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
			ObjectOutputStream output = 
					new ObjectOutputStream(socket.getOutputStream());
			output.writeObject(message);
			output.flush();
		} catch (IOException e) {
			System.err.println("IO Error at sendMessage()");
		}
	}
	
	public void reachedMessage(String name, String value) {
		MessageEvent event = new MessageEvent(this, name, value);
		for (MessageListener l : messageListenerList) {
			l.messageThrow(event);
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
			ObjectInputStream input = 
					new ObjectInputStream(socket.getInputStream());
			while (!socket.isClosed()) {
				String messageSource = (String) input.readObject();
				String[] message = messageSource.split(" ", 2);
				String name = message[0];
				String value = message.length < 2 ? "" : message[1];
				reachedMessage(name, value);
			}
		} catch (IOException e) {
			System.err.println("IO Error at NVCClientUser.run()");
		} catch (ClassNotFoundException e) {
			System.err.println("");
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
				System.err.println("IO Error at messageThrow()");
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
				server.addDiscussion(discussion);
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
			sendMessage(result);
		}
		
		// ENTER: Enter the discussion
		else if (messageType.equals("ENTER")) {
			Discussion discussion = server.getDiscussion(messageValue);
			if (discussion != null) {
				discussion.addUser(this);
				sendMessage("OK");
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
				sendMessage(result);
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
