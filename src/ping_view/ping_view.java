package ping_view;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.SwingConstants;

public class ping_view {

	private JFrame frmPing;
	Thread th1;
	private JTextArea textArea;
	private JTextField textField;
	private JTable table;

	private String system_name = System.getProperty("os.name");

	private Thread[] t1 = new Thread[1];
	private Thread[] t2 = new Thread[1];

	private ExtendedTableModel table_md;

	static Icon icon_red = new ImageIcon(
			Toolkit.getDefaultToolkit().getImage(ping_view.class.getResource("/image/red.png")));
	static Icon icon_green = new ImageIcon(
			Toolkit.getDefaultToolkit().getImage(ping_view.class.getResource("/image/green.png")));
	static Icon icon_grey = new ImageIcon(
			Toolkit.getDefaultToolkit().getImage(ping_view.class.getResource("/image/grey.png")));

//	static Object[][] data={{icon_red,"192.168.0.1","TCP"}};;//內容
	static Object[][] data = {};;// 內容
	static String[] columns = { "status", "IP", "protocol" };// HEAD標題

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {

					ping_view window = new ping_view();
					window.frmPing.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ping_view() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmPing = new JFrame();
		frmPing.setIconImage(Toolkit.getDefaultToolkit().getImage(ping_view.class.getResource("/image/ping.png")));
		frmPing.setTitle("ping小工具");
		frmPing.setBounds(100, 100, 536, 394);
		frmPing.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmPing.getContentPane().setLayout(null);

		textArea = new JTextArea();
//		textArea.setBounds(327, 28, 174, 226);

		JScrollPane scrollpane = new JScrollPane(textArea);
		scrollpane.setBounds(327, 28, 174, 226);
		frmPing.getContentPane().add(scrollpane);

		JButton btnNewButton = new JButton("ICMP ping");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.gc();
				t1 = null;

				String[] st = textArea.getText().replaceAll("((\\r\\n)|\\n)[\\s\\t ]*(\\1)+", "$1").split("\n");// 刪除空行

//				System.out.println(st.length);
				data = new Object[st.length][3];
				t1 = new Thread[st.length];

				try {
					Thread.sleep(250);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				for (int i = 0; i < st.length; i++) {
					data[i][0] = icon_grey;
					data[i][1] = st[i];
					data[i][2] = "null";
				}

				ICMP_ping icmp_ping;

				for (int i = 0; i < st.length; i++) {
					if (!st[i].equals("")) {
						icmp_ping = new ICMP_ping(st[i], i);
						t1[i] = new Thread(icmp_ping);
						t1[i].start();
					}
				}

				table_md = new ExtendedTableModel(data, columns);
				table.setModel(table_md);
				table.getColumnModel().getColumn(0).setPreferredWidth(10);// 列寬
				table.getColumnModel().getColumn(1).setPreferredWidth(80);// 列寬
				table.getColumnModel().getColumn(2).setPreferredWidth(50);// 列寬

			}
		});
		btnNewButton.setBounds(327, 266, 114, 29);
		frmPing.getContentPane().add(btnNewButton);

		JButton btnTcptelnet = new JButton("TCP_ping");
		btnTcptelnet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.gc();
				t2 = null;
				int port = Integer.parseInt(textField.getText());
				String[] st = textArea.getText().replaceAll("((\\r\\n)|\\n)[\\s\\t ]*(\\1)+", "$1").split("\n");// 刪除空行

//				System.out.println(st.length);
				data = new Object[st.length][3];

				for (int i = 0; i < st.length; i++) {
					data[i][0] = icon_grey;
					data[i][1] = st[i];
					data[i][2] = "null";
				}

				TCP_ping tcp_ping;
				Thread t2 = null;
				for (int i = 0; i < st.length; i++) {
					if (!st[i].equals("")) {
						tcp_ping = new TCP_ping(st[i], i, port);
						t2 = new Thread(tcp_ping);
						t2.start();
						System.gc();
					}
				}

				table_md = new ExtendedTableModel(data, columns);
				table.setModel(table_md);
				table.getColumnModel().getColumn(0).setPreferredWidth(10);// 列寬
				table.getColumnModel().getColumn(1).setPreferredWidth(80);// 列寬
				table.getColumnModel().getColumn(2).setPreferredWidth(50);// 列寬
				System.gc();

			}
		});

		btnTcptelnet.setBounds(398, 307, 114, 31);
		frmPing.getContentPane().add(btnTcptelnet);

		textField = new JTextField();
		textField.setHorizontalAlignment(SwingConstants.RIGHT);
		textField.setText("80");
		textField.setBounds(327, 306, 61, 29);
		frmPing.getContentPane().add(textField);
		textField.setColumns(10);

		JLabel lblNewLabel = new JLabel("IP list:");
		lblNewLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		lblNewLabel.setBounds(387, 6, 61, 16);
		frmPing.getContentPane().add(lblNewLabel);

		table_md = new ExtendedTableModel(data, columns);
		table = new JTable(table_md);
		table.setRowHeight(25);// 行高

		table.getColumnModel().getColumn(0).setPreferredWidth(10);// 列寬
		table.getColumnModel().getColumn(1).setPreferredWidth(80);// 列寬
		table.getColumnModel().getColumn(2).setPreferredWidth(50);// 列寬

		JScrollPane scorll2 = new JScrollPane(table);
		scorll2.setBounds(20, 8, 295, 328);
		frmPing.getContentPane().add(scorll2);

		JButton btnNewButton_1 = new JButton("清除");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				t1 = null;
				t2 = null;
				
				textArea.setText("");
				
//				for (int i = 0; i < t1.length; i++) {
////					System.out.println(t1[i].getStackTrace());
//					t1[i].interrupt();	
//					t1[2].interrupt();	
//				}

				data = new Object[1][3];

				for (int i = 0; i < 1; i++) {
					data[i][0] = icon_grey;
					data[i][1] = "";
					data[i][2] = "";
				}

				table_md = new ExtendedTableModel(data, columns);
				table.setModel(table_md);
				table.getColumnModel().getColumn(0).setPreferredWidth(10);// 列寬
				table.getColumnModel().getColumn(1).setPreferredWidth(80);// 列寬
				table.getColumnModel().getColumn(2).setPreferredWidth(50);// 列寬
			}
		});
		btnNewButton_1.setBounds(442, 266, 69, 29);
		frmPing.getContentPane().add(btnNewButton_1);

		table_timer table_t = new table_timer();
		Thread timer1 = new Thread(table_t);
		timer1.start();
	}

	class ICMP_ping implements Runnable {
		private String ip;
		private Boolean getcallback = false;
		private int num;
		int frequency = 0;

		public ICMP_ping(String in_ip, int i) {
			this.ip = in_ip;
			this.num = i;
		}

		@Override
		public void run() {
			while (!getcallback) {
				if (system_name.equals("Mac OS X")) {
					getcallback = ping_fn1(ip);
				} else if (system_name.equals("Windows 10")) {
					getcallback = ping_fn2(ip);
				} else {
					getcallback = ping_fn2(ip);
				}

				if (getcallback) {
					data[num][0] = icon_green;
				} else {
					data[num][0] = icon_red;
				}
				data[num][1] = ip;
				data[num][2] = "ICMP";
				
				if (!getcallback) {
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (frequency >= 1) {
						t1[num].interrupt();
						break;
					}
					frequency++;
					System.out.println(ip + ":嘗試重新連線");
				}
			}
		}

		public Boolean getcallback() {
			return getcallback;
		}

		// mac使用
		private Boolean ping_fn1(String IP) {
			String line = null;
			Boolean b = false;
			int count = 0;
			Process pro = null ;
			BufferedReader buf = null;
			try {
				 pro = Runtime.getRuntime().exec("ping " + IP);
				 buf = new BufferedReader(new InputStreamReader(pro.getInputStream()));
							
				while ((line = buf.readLine()) != null) {
					if (line.indexOf("time=") >= 1) {
						b = true;
//						System.out.println(line);
					} else if (line.indexOf("timeout") >= 1) {
						b = false;
//						System.out.println(line);
					} else {
						b = false;
					}
//					
					if (count >= 1) {
						break;
					}
					count++;
				}
			} catch (Exception ex) {
				System.out.println(ex.getMessage());				
			}
			
			try {
				pro.destroy();	
				buf.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
			return b;
		}

		// windows使用
		private Boolean ping_fn2(String IP) {
			int timeOut = 100; // 響應超時時間
			Boolean b = false;

			try {
				b = InetAddress.getByName(IP).isReachable(timeOut);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // 当返回值是true时，说明host是可用的，false则不可。
			return b;

		}
	}

	class TCP_ping implements Runnable {
		private String ip;
		private int port;
		private Boolean getcallback = false;
		private int num;
		int frequency = 0;

		public TCP_ping(String in_ip, int i, int Port) {
			this.ip = in_ip;
			this.num = i;
			this.port = Port;
		}

		@Override
		public void run() {
			while (!getcallback) {
				getcallback = ping_fn2(ip, port);
				if (getcallback) {
					data[num][0] = icon_green;
				} else {
					data[num][0] = icon_red;
				}
				data[num][1] = ip;
				data[num][2] = "TCP：" + port;
				if (!getcallback) {
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (frequency >= 1) {
						break;
					}
					frequency++;
					System.out.println(ip + ":嘗試重新連線");
				}
			}

		}

		public Boolean getcallback() {
			return getcallback;
		}

		// TCP_ping
		private Boolean ping_fn2(String IP, int Port) {
			Boolean isConnent = false;
			int timeOut = 1000;
			Socket socket = new Socket();
			try {
				socket.connect(new InetSocketAddress(IP, Port), timeOut);
				isConnent = socket.isConnected();
			} catch (IOException e) {
				System.err.println(e.getMessage());
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return isConnent;
		}
	}

	class table_timer implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (true) {
				try {
					Thread.sleep(2500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				table_md = new ExtendedTableModel(data, columns);
				table.setModel(table_md);
				table.getColumnModel().getColumn(0).setPreferredWidth(10);// 列寬
				table.getColumnModel().getColumn(1).setPreferredWidth(80);// 列寬
				table.getColumnModel().getColumn(2).setPreferredWidth(50);// 列寬
			}
		}
	}
}

/** Jtable重新建構類型(可放圖片) */
class ExtendedTableModel extends DefaultTableModel {
	private static final long serialVersionUID = 1L;

	// 重新提供一個構造器，該構造器的實現委託給DefaultTableModel父類
	public ExtendedTableModel(Object[][] cells, String[] columnNames) {
		super(cells, columnNames);
	}

	// 重寫getColumnClass方法，根據每列的第一個值來返回其真實的資料型別
	public Class<? extends Object> getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}
}
