package jp.ac.jaist.skdlab.nvcsys;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstraction of discussion
 * 
 * @author Yutaka Kato
 * @version 0.1.3
 */
public class Discussion implements MessageListener {
	
	private String title = null;
	private NVCClientUser hostUser = null;
	private List<NVCClientUser> userList = null;
	
	public Discussion(String title, NVCClientUser hostUser) {
		userList = new ArrayList<NVCClientUser>();
		this.title = title;
		this.hostUser = hostUser;
		addUser(hostUser);
	}
	
	public String getTitle() {
		return title;
	}
	
	public NVCClientUser getHostUser() {
		return hostUser;
	}
	
	public void addUser(NVCClientUser user) {
		user.addMessageListener(this);
		userList.add(user);
		for (NVCClientUser u : userList) {
			u.reachedMessage("GETU", title);
			u.sendMessage("ENTER " + user.getName());
		}
	}
	
	public boolean containsUser(NVCClientUser user) {
		return userList.contains(user);
	}
	
	public List<NVCClientUser> getUserList() {
		return userList;
	}
	
	public void removeUser(NVCClientUser user) {
		user.removeMessageListener(this);
		userList.remove(user);
		for (NVCClientUser u : userList) {
			u.reachedMessage("GETU", title);
			u.sendMessage("LEAVE " + user.getName());
		}
		
		// Remove discussion
		if (userList.size() == 0) {
			NVCServer.getInstance().removeDiscussion(this);
		}
	}
	
	@Override
	public void messageThrow(MessageEvent e) {
		NVCClientUser source = e.getUser();
		
		if (e.getName().equals("MESSAGE")) {
			for (NVCClientUser u : userList) {
				String message = e.getName() + " " + source.getName() + " " +
						e.getValue();
				u.sendMessage(message);
			}
		}
		
		else if (e.getName().equals("UP_ALL") || 
				e.getName().equals("DOWN_ALL")) {
			for (NVCClientUser u : userList) {
				String message = e.getName() + " " + source.getName();
				u.sendMessage(message);
			}
		}
		
		else if (e.getName().equals("UP")) {
			for (NVCClientUser u : userList) {
//				if (u.getName().equals(e.getValue())) {
					String message = e.getName() + " " + e.getValue();
					u.sendMessage("DOWN_ALL");
					u.sendMessage(message);
//				}
			}
		}
		
		// Change name
		else if (e.getName().equals("CHANGE")) {
			for (NVCClientUser u : userList) {
				u.reachedMessage("GETU", title);
			}
		}
	}
}
