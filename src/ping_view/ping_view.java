package ping_view;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.undo.UndoManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ping_view {

	public static String version = "V1.22"; // 版本號

	private JFrame frmPing;
	Thread th1;
	private JTextArea textArea;

	private JTextField textField;
	private JTable table;
	private UndoManager undoManager;
	private JComboBox<String> modeComboBox; // 下拉選單元件
	private JButton btnGenerateConfig; // 產生 Ubuntu 配置的按鈕
	private ExecutorService executorService = Executors.newFixedThreadPool(15); // // 限制最高同時只有 15 個 Ping 在執行，其餘排隊，防止當機
	private JScrollPane scorll2; // 顯示表格的滾動條 (Ping 模式)
	private JScrollPane networkScrollPane; // 顯示配置的滾動條 (配置模式)
	private JTextArea networkTextArea; // 顯示並可編輯配置的文字區域
	private JTextField NicOneField; // NIC1 網卡名 (例如 eno1)
	private JTextField subnetField1; // 子網路遮罩 (例如 24)
	private JTextField subnetField2; // 子網路遮罩 (例如 24)
	private JTextField NicTwoField; // NIC2 網卡名 (例如 eno2)

//	static String[] authorized = {
//			"tim.flyhighyoga@gmail.com",
//			"alex.ke.scg@gmail.com"
//			};

	static boolean k1 = false;

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
	static String[] columns = { "狀態", "IP", "協定", "延遲" };// HEAD標題

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
		frmPing.setTitle("ping小工具" + version);
		frmPing.setBounds(100, 100, 538, 438);
		frmPing.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmPing.getContentPane().setLayout(null);

//		//授權
//		for(int i=0;i<authorized.length;i++) {
//			if(authorized[i].equals(mac_apple_id())) {
//				k1=true;
//				break;
//			}else {
//				k1=false;
//			}
//		}
//		
//		if(k1) {
//			System.out.println("授權驗證成功");			
//		}else {
//			JOptionPane.showMessageDialog(frmPing,"請透過正規管道獲取軟體","授權失敗！", JOptionPane.ERROR_MESSAGE);
//			System.exit(0);
//			System.out.println("授權驗證失敗");			
//			return;
//		}

		textArea = new JTextArea();
//		textArea.setBounds(327, 28, 174, 226);

		JLabel lblNewLabel = new JLabel("IP List：");
		lblNewLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
		lblNewLabel.setBounds(387, 38, 151, 16);
		frmPing.getContentPane().add(lblNewLabel);

		JScrollPane scrollpane = new JScrollPane(textArea);
		scrollpane.setBounds(327, 58, 184, 226);
		frmPing.getContentPane().add(scrollpane);

		// --- ICMP Ping 按鈕 ---
		JButton btnNewButton = new JButton("ICMP ping");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// 檢查目前模式
				if (modeComboBox.getSelectedIndex() == 1) {// 假設 1 是 Ubuntu 模式
					generateUbuntuNetplan();// 呼叫之前寫的 Ubuntu 配置轉換函數
					return;
				}
				// 清理與解析 IP
				String[] lines = textArea.getText().replaceAll("((\\r\\n)|\\n)[\\s\\t ]*(\\1)+", "$1").split("\n");
				List<String> ipList = new ArrayList<>();
				for (String line : lines) {
					if (!line.trim().isEmpty())
						ipList.addAll(parseIpRange(line.trim()));
				}
				String[] st = ipList.toArray(new String[0]);

				data = new Object[st.length][4];
				for (int i = 0; i < st.length; i++) {
					data[i][0] = icon_grey;
					data[i][1] = st[i];
					data[i][2] = "ICMP";
					data[i][3] = "排隊中-";
				}

				// 初始化表格 (這裡建議直接呼叫 updateTable 封裝函數)
				updateTable();
				for (int i = 0; i < st.length; i++) {
					// 將任務丟進池子裡，它會自動分批 (例如一次只跑 15 個)
					executorService.submit(new ICMP_ping(st[i], i));
				}
			}

		});
		btnNewButton.setBounds(327, 296, 114, 29);
		frmPing.getContentPane().add(btnNewButton);

		// --- TCP Port 按鈕 ---
		JButton btnTcptelnet = new JButton("TCP_port");
		btnTcptelnet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int port = Integer.parseInt(textField.getText());
				String[] lines = textArea.getText().replaceAll("((\\r\\n)|\\n)[\\s\\t ]*(\\1)+", "$1").split("\n");
				List<String> ipList = new ArrayList<>();
				for (String line : lines) {
					if (!line.trim().isEmpty()) ipList.addAll(parseIpRange(line.trim()));
				}
				String[] st = ipList.toArray(new String[0]);

				data = new Object[st.length][4];
				for (int i = 0; i < st.length; i++) {
					data[i][0] = icon_grey;
					data[i][1] = st[i];
					data[i][2] = "TCP:" + port;
					data[i][3] = "排隊中-";
				}
				updateTable();
				for (int i = 0; i < st.length; i++) {
					executorService.submit(new TCP_ping(st[i], i, port));
				}

			}		
		});
		btnTcptelnet.setBounds(398, 337, 114, 31);
		frmPing.getContentPane().add(btnTcptelnet);

		// 端口-方塊
		textField = new JTextField();
		textField.setHorizontalAlignment(SwingConstants.RIGHT);
		textField.setDocument(new NumberDocument());
		textField.setText("80");
		textField.setBounds(327, 336, 61, 29);
		frmPing.getContentPane().add(textField);
		textField.setColumns(10);

		// NIC_1網卡名稱-方塊
		JLabel lblNic1 = new JLabel("NIC1:");
		lblNic1.setBounds(327, 336, 70, 25);
		lblNic1.setVisible(false); // 預設隱藏
		frmPing.getContentPane().add(lblNic1);
		NicOneField = new JTextField("eno1");//
		NicOneField.setBounds(360, 336, 70, 25);
		NicOneField.setVisible(false); // 預設隱藏
		frmPing.getContentPane().add(NicOneField);
		// NIC_1子網段-方塊
		JLabel lblNic1_s = new JLabel("子網:");
		lblNic1_s.setBounds(435, 336, 70, 25);
		lblNic1_s.setVisible(false); // 預設隱藏
		frmPing.getContentPane().add(lblNic1_s);
		subnetField1 = new JTextField("/24");
		subnetField1.setBounds(470, 336, 40, 25);
		subnetField1.setVisible(false);
		frmPing.getContentPane().add(subnetField1);

		// NIC_2網卡名稱-方塊
		JLabel lblNic2 = new JLabel("NIC2:");
		lblNic2.setBounds(327, 366, 70, 25);
		lblNic2.setVisible(false); // 預設隱藏
		frmPing.getContentPane().add(lblNic2);
		NicTwoField = new JTextField("");// 內網網卡名稱-方塊
		NicTwoField.setBounds(360, 366, 70, 25); // 放在原網卡名下方
		NicTwoField.setVisible(false);
		frmPing.getContentPane().add(NicTwoField);
		// NIC_2子網段-方塊
		JLabel lblNic2_s = new JLabel("子網:");
		lblNic2_s.setBounds(435, 366, 70, 25);
		lblNic2_s.setVisible(false); // 預設隱藏
		frmPing.getContentPane().add(lblNic2_s);
		subnetField2 = new JTextField("");
		subnetField2.setBounds(470, 366, 40, 25);
		subnetField2.setVisible(false);
		frmPing.getContentPane().add(subnetField2);

		// --- 左側-檢測後顯示結果欄位 ----
		table_md = new ExtendedTableModel(data, columns);
		table = new JTable(table_md);
		table.setRowHeight(25);// 行高
		table.getColumnModel().getColumn(0).setPreferredWidth(10);// 列寬
		table.getColumnModel().getColumn(1).setPreferredWidth(80);// 列寬
		table.getColumnModel().getColumn(2).setPreferredWidth(20);// 列寬
		table.getColumnModel().getColumn(3).setPreferredWidth(30);// 列寬
		scorll2 = new JScrollPane(table); // 移除前面的 JScrollPane 宣告，賦值給全域變數
		scorll2.setBounds(5, 5, 310, 388);
		frmPing.getContentPane().add(scorll2);

		// --- 左側網路配置編輯區 (networkScrollPane) ---
		networkTextArea = new JTextArea();
		networkTextArea.setEditable(true); // 開啟編輯功能
		networkTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // 等寬字體
		networkTextArea.setComponentPopupMenu(createPopupMenu()); // 讓它也能用右鍵選單

		networkScrollPane = new JScrollPane(networkTextArea);
		networkScrollPane.setBounds(5, 5, 310, 388); // 與表格座標重疊
		networkScrollPane.setVisible(false); // 預設隱藏
		frmPing.getContentPane().add(networkScrollPane);

		// 清除按鈕
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

				data = new Object[1][4];

				for (int i = 0; i < 1; i++) {
					data[i][0] = icon_grey;
					data[i][1] = "";
					data[i][2] = "";
					data[i][3] = "";
				}

				updateTable();
			}
		});
		btnNewButton_1.setBounds(442, 296, 69, 29);
		frmPing.getContentPane().add(btnNewButton_1);

		// 下拉選單 (模式切換)
		String[] modes = { "Ping & TCP 監測模式", "Ubuntu 網卡配置模式","Debian(ipv4-舊) 網卡配置模式" , "Debian(ipv4+ipv6) 網卡配置模式", "Windows 批次檔網卡配置模式","Centos 6/7 網卡配置模式", "Centos 8/9 ,Rocky 網卡配置模式", "IP Addr 指令模式","整理IP-多個IP整理成一行方法" };
		modeComboBox = new JComboBox<>(modes);
		modeComboBox.setBounds(327, 5, 185, 25);
		frmPing.getContentPane().add(modeComboBox);

		// 生產網卡 配置按鈕 (預設隱藏)
		btnGenerateConfig = new JButton("產生網卡配置文件");
		btnGenerateConfig.setBounds(327, 296, 185, 29);
		btnGenerateConfig.setVisible(false);
		frmPing.getContentPane().add(btnGenerateConfig);

		// 模式切換監聽器
		modeComboBox.addActionListener(e -> {
		    int selectedIndex = modeComboBox.getSelectedIndex();
		    String selected = (String) modeComboBox.getSelectedItem();
		    
		    boolean isPingMode = (selectedIndex == 0); // 假設第一項是 Ping 模式

		    // 1. 基本控制項顯示/隱藏
		    btnNewButton.setVisible(isPingMode);
		    btnNewButton_1.setVisible(isPingMode);
		    btnTcptelnet.setVisible(isPingMode);
		    textField.setVisible(isPingMode);
		    btnGenerateConfig.setVisible(!isPingMode);

		    // 2. 左側大區塊切換 (表格 vs 編輯器)
		    if (isPingMode) {
		        scorll2.setVisible(true);
		        networkScrollPane.setVisible(false);
		    } else {
		        scorll2.setVisible(false);
		        networkScrollPane.setVisible(true);
		    }

		    // 3. 端口&網卡&子網方塊顯示邏輯
		    lblNic1.setVisible(!isPingMode);
		    lblNic1_s.setVisible(!isPingMode);
		    lblNic2.setVisible(!isPingMode);
		    NicOneField.setVisible(!isPingMode);
		    subnetField1.setVisible(!isPingMode);
		    NicTwoField.setVisible(!isPingMode);

		    // --- 關鍵新增：針對 Windows 模式自動填寫網卡名稱 ---
	        switch (selectedIndex) {
            case 1: //Ubuntu
            	NicOneField.setText("eno1");
                break;
            case 2: //Debian(ipv4)
            	NicOneField.setText("eth0");
                break;
            case 3: //Debian(ipv4+ipv6)
            	NicOneField.setText("eth0");
                break;	        
            case 4: //windows
            	 NicOneField.setText("以太网");               
                break;
            case 5: //Centos7
            	NicOneField.setText("eno1");               
               break;
            case 6: //Rocky Linux 
            	NicOneField.setText("eno1");               
               break;
            case 7: // IP Addr 指令
            	NicOneField.setText("eno1");
                break;
            case 8: //整理IP-多個IP整理成一行方法
                break;
            default:
            	break;	
	        }

		});

		// 網卡配置邏輯
		btnGenerateConfig.addActionListener(e -> {
		    int mode = modeComboBox.getSelectedIndex();
	        switch (mode) {
	            case 1: //Ubuntu
	                generateUbuntuNetplan();
	                break;
	            case 2: //Debian(ipv4)
	                generateDebianAliasConfig();
	                break;
	            case 3: //Debian(ipv4+ipv6)
	                generateDebianInterfaces();
	                break;	        
	            case 4: //windows
	                generateWindowsBatchConfig();	                
	                break;
	            case 5: //Centos7
	            	generateCentOS7Config();	                
	                break;
	            case 6: //Rocky Linux 
	            	generateRockyLinuxConfig();       
	               break;
	            case 7: // IP Addr 指令
	                generateIpAddrConfig();
	                break;
	            case 8: // 整理IP-多個IP整理成一行方法
	            	compactIpAndCount();
	                break;   	               
	            default:
	                break;
	        }
		});

		// 右鍵功能表 和初始化UndoManager
		JPopupMenu popupMenu = createPopupMenu();
		textArea.setComponentPopupMenu(popupMenu);// IP List:用
		undoManager = new UndoManager();
		textArea.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));// 快捷鍵
		networkTextArea.setComponentPopupMenu(popupMenu);
		;// 讓產出的結果框也能按右鍵複製

		// 按鈕行為_快捷鍵
		setupKeyboardShortcuts(textArea);
		setupKeyboardShortcuts(networkTextArea);

//		//每xx秒更新一次table
//		table_timer table_t = new table_timer();
//		Thread timer1 = new Thread(table_t);
//		timer1.start();
	}

	// mac 電腦取得apple ID 方法(shell指令)
	public static String mac_apple_id() {
		String line = "";
		String value = "";
		try {
			String[] command = { "/bin/sh", "-c",
					"defaults read MobileMeAccounts Accounts | grep AccountID | cut -d \\\" -f2" };
			Process pro = Runtime.getRuntime().exec(command);
			BufferedReader buf = new BufferedReader(new InputStreamReader(pro.getInputStream()));

			while ((line = buf.readLine()) != null) {
//				System.out.println(line);
				value = line;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return value;
	}

	// --- ICMP 執行緒類別 ---
	class ICMP_ping implements Runnable {
		private String ip;
		private int num;

		public ICMP_ping(String ip, int num) {
			this.ip = ip;
			this.num = num;
		}

		@Override
		public void run() {
			String resultTime = "timeout";
			boolean success = false;
			int timeout = 500; 

			// 判斷是否為 IPv6 (包含冒號即視為 IPv6)
			boolean isIPv6 = ip.contains(":");

			if (system_name.contains("Mac")) {
				// 將 isIPv6 傳入判斷指令
				resultTime = ping_mac(ip, timeout, isIPv6);
				success = !resultTime.equals("timeout");
			} else {
				// Windows/Linux 邏輯維持不變
				long start = System.currentTimeMillis();
				try {
					success = InetAddress.getByName(ip).isReachable(timeout);
					if (success)
						resultTime = (System.currentTimeMillis() - start) + " ms";
				} catch (IOException e) {
					success = false;
				}
			}

			data[num][0] = success ? icon_green : icon_red;
			data[num][3] = resultTime;
			table_md.fireTableRowsUpdated(num, num);
			updateTable();
		}

		private String ping_mac(String ip, int timeout, boolean isIPv6) {
		    String line;
		    try {
		        List<String> command = new ArrayList<String>();
		        if (isIPv6) {
		            // Mac 的 ping6 不支援 -W [ms/s]，所以只給 -c 1
		            command.add("ping6");
		            command.add("-c");
		            command.add("1");
		        } else {
		            // Mac 的 ping 支援 -W [ms]
		            command.add("ping");
		            command.add("-c");
		            command.add("1");
		            command.add("-W");
		            command.add(String.valueOf(timeout));
		        }
		        command.add(ip);

		        ProcessBuilder pb = new ProcessBuilder(command);
		        pb.redirectErrorStream(true); 
		        Process pro = pb.start();

		        // 關鍵：使用 BufferedReader 讀取，但要防止它卡住
		        BufferedReader buf = new BufferedReader(new InputStreamReader(pro.getInputStream()));
		        
		        // 另外開一個執行續或簡單判斷時間
		        long startTime = System.currentTimeMillis();

		        while (true) {
		            // 檢查是否超時 (給予比 timeout 稍微寬鬆一點的時間)
		            if (System.currentTimeMillis() - startTime > timeout + 200) {
		                pro.destroy(); // 強制終止行程
		                break;
		            }

		            if (buf.ready()) { // 只有在有資料時才讀取，避免 readline 阻塞
		                line = buf.readLine();
		                if (line == null) break;
		                
		                if (line.contains("time=")) {
		                    int start = line.indexOf("time=") + 5;
		                    int end = line.indexOf(" ms", start);
		                    if (end != -1) {
		                        String timeStr = line.substring(start, end).trim();
		                        if (timeStr.contains(".")) {
		                            timeStr = timeStr.split("\\.")[0];
		                        }
		                        return timeStr + " ms";
		                    }
		                }
		            } else {
		                Thread.sleep(10); // 稍微休息一下，避免 CPU 飆高
		            }
		        }
		    } catch (Exception e) {
		        return "timeout";
		    }
		    return "timeout";
		}
	}

	// --- TCP 執行緒類別 ---
	class TCP_ping implements Runnable {
		private String ip;
		private int port;
		private int num;

		public TCP_ping(String ip, int num, int port) {
			this.ip = ip;
			this.num = num;
			this.port = port;
		}

		@Override
		public void run() {
			int timeout = 500; // 硬編碼 500ms
			long start = System.currentTimeMillis();
			boolean success = false;
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(ip.trim(), port), timeout);
				success = socket.isConnected();
			} catch (IOException e) {
				success = false;
			}

			data[num][0] = success ? icon_green : icon_red;
			data[num][3] = success ? (System.currentTimeMillis() - start) + " ms" : "timeout";
			table_md.fireTableRowsUpdated(num, num);
			updateTable();
		}
	}

	//每xx秒自動更新UI (不使用了)
	// 每xx秒更新一次table
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
//				table_md = new ExtendedTableModel(data, columns);
//				table.setModel(table_md);
//				table.getColumnModel().getColumn(0).setPreferredWidth(10);// 列寬
//				table.getColumnModel().getColumn(1).setPreferredWidth(80);// 列寬
//				table.getColumnModel().getColumn(2).setPreferredWidth(20);// 列寬
//				table.getColumnModel().getColumn(2).setPreferredWidth(30);// 列寬
				updateTable();

			}
		}
	}

	//解析IP資訊規則
	// 方法：解析單行 IP 或範圍，並展開 (支援 IPv6)
	public static List<String> parseIpRange(String input) {
	    List<String> ipList = new ArrayList<String>();
	    input = input.trim();
	    if (input.isEmpty()) return ipList;

	    try {
	        // 處理包含斜線的列舉情況 (例如 40-41/53)
	        if (input.contains("/")) {
	            String base = input.substring(0, input.lastIndexOf(".") + 1);
	            String tails = input.substring(input.lastIndexOf(".") + 1);
	            String[] parts = tails.split("/");
	            
	            for (String p : parts) {
	                // 遞迴調用或直接處理裡面的範圍 (例如 p 可能是 "40-41" 或 "53")
	                if (p.contains("-")) {
	                    String[] rangeParts = p.split("-");
	                    int start = Integer.parseInt(rangeParts[0]);
	                    int end = Integer.parseInt(rangeParts[1]);
	                    for (int i = start; i <= end; i++) {
	                        ipList.add(base + i);
	                    }
	                } else {
	                    ipList.add(base + p);
	                }
	            }
	        } 
	        // 處理單純的範圍 (例如 192.168.1.1-20)
	        else if (input.contains("-")) {
	            String[] parts = input.split("-");
	            String startIp = parts[0].trim();
	            String endPart = parts[1].trim();

	            if (startIp.contains(":")) {
	                // IPv6 範圍處理 (16進位)
	                String base = startIp.substring(0, startIp.lastIndexOf(":") + 1);
	                int start = Integer.parseInt(startIp.substring(startIp.lastIndexOf(":") + 1), 16);
	                int end = Integer.parseInt(endPart, 16);
	                for (int i = start; i <= end; i++) {
	                    ipList.add(base + Integer.toHexString(i));
	                }
	            } else {
	                // IPv4 範圍處理
	                String base = startIp.substring(0, startIp.lastIndexOf(".") + 1);
	                int start = Integer.parseInt(startIp.substring(startIp.lastIndexOf(".") + 1));
	                int end = Integer.parseInt(endPart);
	                for (int i = start; i <= end; i++) {
	                    ipList.add(base + i);
	                }
	            }
	        } else {
	            // 單個 IP
	            ipList.add(input);
	        }
	    } catch (Exception e) {
	        System.err.println("解析失敗: " + input);
	    }
	    return ipList;
	}

	//更新UI介面
	// 將重複的 UI 更新邏輯集中在這裡
	private void updateTable() {
		// 使用 SwingUtilities 確保在 UI 執行緒更新，防止畫面閃爍或崩潰
		SwingUtilities.invokeLater(() -> {
			table_md = new ExtendedTableModel(data, columns);
			table.setModel(table_md);
			table.getColumnModel().getColumn(0).setPreferredWidth(10);// 列寬
			table.getColumnModel().getColumn(1).setPreferredWidth(80);// 列寬
			table.getColumnModel().getColumn(2).setPreferredWidth(20);// 列寬
			table.getColumnModel().getColumn(3).setPreferredWidth(30);// 列寬
		});
	}

	//Ubtun生產配置方法
	// Ubuntu 格式產生邏輯
	private void generateUbuntuNetplan() {
		String input = textArea.getText().trim();
		if (input.isEmpty())
			return;

		// 取得 NIC1 參數 (外網)
		String nic1Name = NicOneField.getText().trim();
		String nic1Mask = formatSubnet(subnetField1.getText().trim());

		// 取得 NIC2 參數 (內網)
		String nic2Name = NicTwoField.getText().trim();

		StringBuilder sb = new StringBuilder();
		sb.append("# This is the network config written by 'subiquity'\n");
		sb.append("network:\n  version: 2\n  renderer: networkd\n  ethernets:\n");

		// --- 1. 處理 NIC1 (外網) ---
		sb.append("    ").append(nic1Name).append(":\n");
		sb.append("      dhcp4: no\n      dhcp6: no\n");

		List<String> allIps = new ArrayList<>();
		String[] lines = input.split("\n");
		for (String line : lines) {
			allIps.addAll(parseIpRange(line.trim()));
		}

		// NIC1 地址與動態網關計算
		sb.append("      addresses: [ ");
		String gv4 = "", gv6 = "";
		boolean hasV6 = false;

		// --- 在遍歷 allIps 的循環中 ---
		for (int i = 0; i < allIps.size(); i++) {
			String ip = allIps.get(i);
			if (ip.contains(":")) {
				sb.append(ip).append("/112");
				hasV6 = true;
				if (gv6.isEmpty()) {
					// IPv6 依照你的規則維持 :1 或是同樣邏輯
					gv6 = ip.substring(0, ip.lastIndexOf(":")) + ":1";
				}
			} else {
				sb.append(ip).append(nic1Mask);
				if (gv4.isEmpty()) {
					// --- 重點：根據 CIDR 計算第一個可用 IP ---
					int cidr = Integer.parseInt(nic1Mask.replace("/", ""));
					gv4 = calculateFirstAvailableIp(ip, cidr);
				}
			}
			if (i < allIps.size() - 1)
				sb.append(", ");
		}
		sb.append(" ]\n");

		// 路由設定 (這裡的 via 現在會隨 IP 變化了)
		sb.append("      routes:\n");
		if (!gv4.isEmpty()) {
			sb.append("        - to: default\n          via: ").append(gv4).append("\n");
		}
		if (hasV6 && !gv6.isEmpty()) {
			sb.append("        - to: \"::/0\"\n          via: ").append(gv6).append("\n");
		}

		// DNS 設定
		sb.append("      nameservers:\n          addresses: [ 8.8.8.8, 1.1.1.1");
		if (hasV6)
			sb.append(", 2001:4860:4860::8888");
		sb.append(" ]\n");

		// --- 2. 處理 NIC2 (內網) ---
		// 規則：NIC2 不為空才產出，且固定為 192.168.100.10/24，不需計算
		if (!nic2Name.isEmpty()) {
			sb.append("    ").append(nic2Name).append(":\n");
			sb.append("      dhcp4: no\n");
			sb.append("      addresses: [ 192.168.100.10/24 ]\n");
		}

		networkTextArea.setText(sb.toString());
		networkTextArea.setCaretPosition(0);
	}

	//Debian ipv4生產配置方法
	private void generateDebianAliasConfig() {
	    String input = textArea.getText().trim();
	    if (input.isEmpty()) return;

	    String nic1Name = NicOneField.getText().trim();
	    String nic1Mask = formatSubnet(subnetField1.getText().trim());
	    int cidr = Integer.parseInt(nic1Mask.replace("/", ""));

	    // 1. 解析 IP 並分類
	    List<String> allIps = new ArrayList<>();
	    for (String line : input.split("\n")) {
	        allIps.addAll(parseIpRange(line.trim()));
	    }

	    List<String> ipv4s = allIps.stream()
	                               .filter(ip -> !ip.contains(":"))
	                               .collect(Collectors.toList());

	    StringBuilder sb = new StringBuilder();

	    // --- 處理 IPv4 別名格式 ---
	    for (int i = 0; i < ipv4s.size(); i++) {
	        String currentIp = ipv4s.get(i);
	        String aliasSuffix = (i == 0) ? "" : ":" + (i - 1); // 第一個 IP 不帶後綴，之後是 :0, :1...
	        String fullNicName = nic1Name + aliasSuffix;

	        sb.append("auto ").append(fullNicName).append("\n");
	        sb.append("iface ").append(fullNicName).append(" inet static\n");
	        sb.append("        address ").append(currentIp).append(nic1Mask).append("\n");
	        
	        // 依照你的範例，每一組都加上 Gateway
	        String gv4 = calculateFirstAvailableIp(currentIp, cidr);
	        sb.append("        gateway ").append(gv4).append("\n");

	        // 只有第一個主網卡加上 DNS
	        if (i == 0) {
	            sb.append("        dns-nameservers 1.1.1.1\n");
	            sb.append("        dns-search 8.8.8.8\n");
	        }
	        sb.append("\n");
	    }

	    networkTextArea.setText(sb.toString());
	    networkTextArea.setCaretPosition(0);
	}
	
	//Debian ipv4+ipv6生產配置方法
	private void generateDebianInterfaces() {
	    String input = textArea.getText().trim();
	    if (input.isEmpty()) return;

	    String nic1Name = NicOneField.getText().trim();
	    String nic1Mask = formatSubnet(subnetField1.getText().trim());
	    int cidr = Integer.parseInt(nic1Mask.replace("/", ""));

	    List<String> ips = new ArrayList<>();
	    for (String line : input.split("\n")) ips.addAll(parseIpRange(line.trim()));

	    StringBuilder sb = new StringBuilder();
	    sb.append("auto ").append(nic1Name).append("\n");

	    // --- 處理 IPv4 ---
	    List<String> ipv4s = ips.stream().filter(ip -> !ip.contains(":")).collect(Collectors.toList());
	    if (!ipv4s.isEmpty()) {
	        String firstV4 = ipv4s.get(0);
	        String gv4 = calculateFirstAvailableIp(firstV4, cidr);
	        
	        sb.append("iface ").append(nic1Name).append(" inet static\n");
	        sb.append("        address ").append(firstV4).append(nic1Mask).append("\n");
	        sb.append("        gateway ").append(gv4).append("\n");
	        sb.append("        dns-nameservers 8.8.8.8 1.1.1.1\n"); // 修正 DNS 寫法

	        // 其餘 IPv4 使用 up ip addr add
	        for (int i = 1; i < ipv4s.size(); i++) {
	            sb.append("        up ip addr add ").append(ipv4s.get(i)).append(nic1Mask)
	              .append(" dev ").append(nic1Name).append("\n");
	        }
	    }

	    sb.append("\n");

	    // --- 處理 IPv6 ---
	    List<String> ipv6s = ips.stream().filter(ip -> ip.contains(":")).collect(Collectors.toList());
	    if (!ipv6s.isEmpty()) {
	        String firstV6 = ipv6s.get(0);
	        String gv6 = firstV6.substring(0, firstV6.lastIndexOf(":") + 1) + "1";

	        sb.append("iface ").append(nic1Name).append(" inet6 static\n");
	        sb.append("        address ").append(firstV6).append("\n");
	        sb.append("        netmask 112\n");
	        sb.append("        gateway ").append(gv6).append("\n");
	        
	        // 直接寫入常用的 IPv6 DNS
	        sb.append("        dns-nameservers 2001:4860:4860::8888 2001:4860:4860::8844\n");

	        // 其餘 IPv6 使用 up ip -6 addr add
	        for (int i = 1; i < ipv6s.size(); i++) {
	            sb.append("        up ip -6 addr add ").append(ipv6s.get(i)).append("/112")
	              .append(" dev ").append(nic1Name).append("\n");
	        }
	    }

	    networkTextArea.setText(sb.toString());
	    networkTextArea.setCaretPosition(0);
	}
	
	//Windows 生產配置方法
	private void generateWindowsBatchConfig() {
		String input = textArea.getText().trim();
	    if (input.isEmpty()) return;

	    String nicName = NicOneField.getText().trim();
	    
	    // --- 關鍵修正：從 UI 獲取 CIDR 並轉換 ---
	    String cidrInput = subnetField1.getText().trim(); // 假設你填 /27
	    String mask = cidrToMask(cidrInput); 
	    int prefix = Integer.parseInt(cidrInput.replace("/", "").trim());

	    List<String> allIps = new ArrayList<String>();
	    for (String line : input.split("\n")) {
	        if (!line.trim().isEmpty()) {
	            allIps.addAll(parseIpRange(line.trim()));
	        }
	    }

	    StringBuilder sb = new StringBuilder();
	    sb.append("@echo off\n");
	    sb.append("echo Setting IPv4 and IPv6 for: ").append(nicName).append("\n\n");

	    int v4Count = 0;
	    int v6Count = 0;

	    for (String ip : allIps) {
	        if (ip.contains(":")) {
	            // --- IPv6 部分 ---
	            if (v6Count == 0) {
	                // 1. 先設定第一個地址 (注意：set 指令沒有 gateway 參數)
	                sb.append("netsh interface ipv6 set address interface=\"").append(nicName)
	                  .append("\" address=").append(ip).append("/112 store=persistent\n");
	                
	                // 2. 另外新增 IPv6 預設路由 (這才是 Windows 的 Gateway 寫法)
	                String gv6 = ip.substring(0, ip.lastIndexOf(":") + 1) + "1";
	                sb.append("netsh interface ipv6 add route prefix=::/0 interface=\"").append(nicName)
	                  .append("\" nexthop=").append(gv6).append(" publish=yes\n");
	                
	                // 3. 設定 DNS (加上 validate=no 避免報錯)
	                sb.append("netsh interface ipv6 set dnsservers name=\"").append(nicName)
	                  .append("\" static 2001:4860:4860::8888 primary validate=no\n");
	            } else {
	                sb.append("netsh interface ipv6 add address interface=\"").append(nicName)
	                  .append("\" address=").append(ip).append("/112\n");
	            }
	            v6Count++;
	        } else {
	        	// --- IPv4 部分 ---
	            if (v4Count == 0) {
	                // 自動計算 Gateway (傳入 prefix 確保計算正確)
	                String gv4 = calculateFirstAvailableIp(ip, prefix);
	                
	                // 使用轉換後的 mask
	                sb.append("netsh interface ip set address name=\"").append(nicName)
	                  .append("\" static ").append(ip).append(" ").append(mask)
	                  .append(" ").append(gv4).append("\n");
	                
	                sb.append("netsh interface ip set dnsservers name=\"").append(nicName)
	                  .append("\" static 8.8.8.8 primary validate=no\n");
	            } else {
	                // 後續 IP 同樣使用正確的遮罩
	                sb.append("netsh interface ip add address \"").append(nicName)
	                  .append("\" ").append(ip).append(" ").append(mask).append("\n");
	            }
	            v4Count++;
	        }
	    }

	    sb.append("\necho Finished!\npause");
	    networkTextArea.setText(sb.toString());
	    networkTextArea.setCaretPosition(0);
	}
	
	//Centos7 生產配置方法
	private void generateCentOS7Config() {
	    String input = textArea.getText().trim();
	    if (input.isEmpty()) return;

	    String nicName = NicOneField.getText().trim();
	    String cidrInput = subnetField1.getText().trim();
	    String mask = cidrToMask(cidrInput);
	    int prefix = Integer.parseInt(cidrInput.replace("/", "").trim());

	    List<String> rawLines = Arrays.asList(input.split("\n"));
	    List<List<String>> ipv4Ranges = new ArrayList<List<String>>();
	    List<String> ipv6s = new ArrayList<String>();

	    for (String line : rawLines) {
	        line = line.trim();
	        if (line.isEmpty()) continue;
	        if (line.contains(":")) {
	            ipv6s.addAll(parseIpRange(line));
	        } else {
	            List<String> segment = parseIpRange(line);
	            if (!segment.isEmpty()) {
	                ipv4Ranges.add(segment);
	            }
	        }
	    }

	    StringBuilder mainSb = new StringBuilder(); // 主配置文件內容
	    StringBuilder rangeSb = new StringBuilder(); // Range 文件內容
	    
	    // --- 準備主配置文件 ---
	    mainSb.append("# --- 網卡主配置文件 ---\n");
	    mainSb.append("cat <<EOF > /etc/sysconfig/network-scripts/ifcfg-").append(nicName).append("\n");
	    mainSb.append("DEVICE=\"").append(nicName).append("\"\n");
	    mainSb.append("NAME=\"").append(nicName).append("\"\n");
	    mainSb.append("ONBOOT=yes\n");
	    mainSb.append("BOOTPROTO=static\n");

	    int currentCloneNum = 0; // 虛擬網卡起始編號 (alias)
	    int rangeFileIdx = 0;
	    int extraIpIdx = 0; // 用於 IPADDRN 的計數器

	    // --- 處理 IPv4 ---
	    if (!ipv4Ranges.isEmpty()) {
	        for (int i = 0; i < ipv4Ranges.size(); i++) {
	            List<String> currentSegment = new ArrayList<String>(ipv4Ranges.get(i));
	            
	            // 邏輯判斷：如果是第一行且第一個 IP，設為主 IP (IPADDR)
	            if (i == 0 && !currentSegment.isEmpty()) {
	                String primaryV4 = currentSegment.get(0);
	                String gv4 = calculateFirstAvailableIp(primaryV4, prefix);
	                mainSb.append("IPADDR=").append(primaryV4).append("\n");
	                mainSb.append("NETMASK=").append(mask).append("\n");
	                mainSb.append("GATEWAY=").append(gv4).append("\n");
	                currentSegment.remove(0); // 移除已使用的主 IP
	            }

	            if (currentSegment.isEmpty()) continue;

	            // 判斷剩下的 IP 是要寫入主文件還是寫入 Range 文件
	            // 規則：如果該行剩餘數量 <= 2，直接寫入主文件的 IPADDR_N
	            if (currentSegment.size() <= 2) {
	                for (String ip : currentSegment) {
	                    extraIpIdx++;
	                    mainSb.append("IPADDR").append(extraIpIdx).append("=").append(ip).append("\n");
	                    mainSb.append("NETMASK").append(extraIpIdx).append("=").append(mask).append("\n");
	                    // 注意：IPADDRn 模式在舊版系統不需要手動算 CLONENUM，系統會自動處理
	                }
	            } else {
	                // 數量較多，使用 Range 文件
	                rangeSb.append("# --- IPv4 Range段: ").append(rangeFileIdx).append(" ---\n");
	                rangeSb.append("cat <<EOF > /etc/sysconfig/network-scripts/ifcfg-").append(nicName)
	                       .append("-range").append(rangeFileIdx).append("\n");
	                rangeSb.append("IPADDR_START=").append(currentSegment.get(0)).append("\n");
	                rangeSb.append("IPADDR_END=").append(currentSegment.get(currentSegment.size() - 1)).append("\n");
	                rangeSb.append("NETMASK=").append(mask).append("\n");
	                rangeSb.append("CLONENUM_START=").append(currentCloneNum).append("\n");
	                rangeSb.append("ARPCHECK=no\n");
	                rangeSb.append("EOF\n\n");
	                
	                // 累加編號：Range 文件佔用的數量
	                currentCloneNum += currentSegment.size();
	                rangeFileIdx++;
	            }
	        }
	    }

	    mainSb.append("DNS1=8.8.8.8\nDNS2=1.1.1.1\nARPCHECK=no\nNM_CONTROLLED=no\nZONE=public\n");

	    // --- 處理 IPv6 ---
	    if (!ipv6s.isEmpty()) {
	        mainSb.append("IPV6INIT=yes\nIPV6_AUTOCONF=no\nIPV6_DEFROUTE=yes\nIPV6_FAILURE_FATAL=yes\n");
	        mainSb.append("IPV6ADDR=").append(ipv6s.get(0)).append("/112\n");
	        if (ipv6s.size() > 1) {
	            mainSb.append("IPV6ADDR_SECONDARIES=\"");
	            for (int i = 1; i < ipv6s.size(); i++) {
	                mainSb.append(ipv6s.get(i)).append("/112").append(i == ipv6s.size() - 1 ? "" : " ");
	            }
	            mainSb.append("\"\n");
	        }
	        String gv6 = ipv6s.get(0).substring(0, ipv6s.get(0).lastIndexOf(":") + 1) + "1";
	        mainSb.append("IPV6_DEFAULTGW=").append(gv6).append("\n");
	    }
	    mainSb.append("EOF\n\n");

	    // 最後組合輸出
	    networkTextArea.setText(mainSb.toString() + rangeSb.toString() + "systemctl restart network\n");
	    networkTextArea.setCaretPosition(0);
	}
	
	//Rocky Linux 生產配置方法
	private void generateRockyLinuxConfig() {
	    String input = textArea.getText().trim();
	    if (input.isEmpty()) return;

	    String nicName = NicOneField.getText().trim();
	    String cidrInput = subnetField1.getText().trim();
	    int prefix = Integer.parseInt(cidrInput.replace("/", "").trim());

	    // 1. 解析所有 IP 並分類 (Java 8)
	    List<String> allIps = new ArrayList<String>();
	    for (String line : input.split("\n")) {
	        if (!line.trim().isEmpty()) {
	            allIps.addAll(parseIpRange(line.trim()));
	        }
	    }

	    List<String> ipv4s = new ArrayList<String>();
	    List<String> ipv6s = new ArrayList<String>();
	    for (String ip : allIps) {
	        if (ip.contains(":")) ipv6s.add(ip);
	        else ipv4s.add(ip);
	    }

	    StringBuilder sb = new StringBuilder();
	    
	    // 生成部署用的腳本格式
	    sb.append("# --- Rocky Linux 9 NetworkManager 配置腳本 ---\n");
	    sb.append("cat <<EOF > /etc/NetworkManager/system-connections/").append(nicName).append(".nmconnection\n");
	    
	    sb.append("[connection]\n");
	    sb.append("id=").append(nicName).append("\n");
	    sb.append("type=ethernet\n");
	    sb.append("autoconnect=true\n");
	    sb.append("interface-name=").append(nicName).append("\n\n");

	    sb.append("[ethernet]\n\n");

	    // --- IPv4 區塊 ---
	    sb.append("[ipv4]\n");
	    if (!ipv4s.isEmpty()) {
	        for (int i = 0; i < ipv4s.size(); i++) {
	            if (i == 0) {
	                // 第一個位址包含 Gateway
	                String gv4 = calculateFirstAvailableIp(ipv4s.get(i), prefix);
	                sb.append("address1=").append(ipv4s.get(i)).append("/").append(prefix).append(",").append(gv4).append("\n");
	            } else {
	                sb.append("address").append(i + 1).append("=").append(ipv4s.get(i)).append("/").append(prefix).append("\n");
	            }
	        }
	    }
	    sb.append("dns=8.8.8.8;1.1.1.1;\n");
	    sb.append("may-fail=false\n");
	    sb.append("method=manual\n\n");

	    // --- IPv6 區塊 ---
	    sb.append("[ipv6]\n");
	    if (!ipv6s.isEmpty()) {
	        sb.append("method=manual\n");
	        for (int i = 0; i < ipv6s.size(); i++) {
	            sb.append("address").append(i + 1).append("=").append(ipv6s.get(i)).append("/112\n");
	        }
	        String gv6 = ipv6s.get(0).substring(0, ipv6s.get(0).lastIndexOf(":") + 1) + "1";
	        sb.append("gateway=").append(gv6).append("\n");
	        sb.append("dns=2001:4860:4860::8888;\n");
	    } else {
	        // 沒有 IPv6 地址時，必須設為 ignore 避免啟動錯誤
	        sb.append("method=ignore\n");
	    }
	    
	    sb.append("\n[proxy]\n");
	    sb.append("EOF\n\n");

	    // --- 權限與重啟指令 ---
	    sb.append("chmod 600 /etc/NetworkManager/system-connections/").append(nicName).append(".nmconnection\n");
	    sb.append("nmcli connection load /etc/NetworkManager/system-connections/").append(nicName).append(".nmconnection\n");
	    sb.append("nmcli connection up ").append(nicName).append("\n");

	    networkTextArea.setText(sb.toString());
	    networkTextArea.setCaretPosition(0);
	}
	
	//ip addr 指令 生產配置指令
	private void generateIpAddrConfig() {
	    String input = textArea.getText().trim();
	    if (input.isEmpty()) return;

	    String nicName = NicOneField.getText().trim();
	    String cidrInput = subnetField1.getText().trim();
	    int prefix = Integer.parseInt(cidrInput.replace("/", "").trim());

	    // 1. 解析所有 IP 並展開
	    List<String> allIps = new ArrayList<String>();
	    for (String line : input.split("\n")) {
	        if (!line.trim().isEmpty()) {
	            allIps.addAll(parseIpRange(line.trim()));
	        }
	    }

	    StringBuilder sb = new StringBuilder();
	    sb.append("# --- Linux IP Command 即時生效腳本 ---\n");
	    sb.append("# 注意：此指令直接修改記憶體，重啟網卡或重載配置後會失效\n\n");

	    String gatewayV4 = "";
	    String gatewayV6 = "";

	    for (String ip : allIps) {
	        if (ip.contains(":")) {
	            // IPv6 邏輯
	            sb.append("ip -6 addr add ").append(ip).append("/112 dev ").append(nicName).append("\n");
	            if (gatewayV6.isEmpty()) {
	                gatewayV6 = ip.substring(0, ip.lastIndexOf(":") + 1) + "1";
	            }
	        } else {
	            // IPv4 邏輯
	            sb.append("ip addr add ").append(ip).append("/").append(prefix).append(" dev ").append(nicName).append("\n");
	            if (gatewayV4.isEmpty()) {
	                gatewayV4 = calculateFirstAvailableIp(ip, prefix);
	            }
	        }
	    }

	    sb.append("\n# --- 路由配置 ---\n");
	    if (!gatewayV4.isEmpty()) {
	        sb.append("ip route add default via ").append(gatewayV4).append(" dev ").append(nicName).append(" onlink\n");
	    }
	    if (!gatewayV6.isEmpty()) {
	        sb.append("ip -6 route add default via ").append(gatewayV6).append(" dev ").append(nicName).append("\n");
	    }

	    networkTextArea.setText(sb.toString());
	    networkTextArea.setCaretPosition(0);
	}
	
	//整理IP-多個IP整理成一行方法
	private void compactIpAndCount() {
	    String input = textArea.getText().trim();
	    if (input.isEmpty()) return;

	    // 1. 展開並去重、排序
	    List<String> allIps = new ArrayList<String>();
	    for (String line : input.split("\n")) {
	        if (!line.trim().isEmpty()) {
	            // parseIpRange 會處理 192.168.1.1-5 這種格式並展開成清單
	            allIps.addAll(parseIpRange(line.trim()));
	        }
	    }

	    // 2. 統計總數 (包含重複的話可以用 Set 去重，這裡假設 James 要的是展開後的總量)
	    int totalCount = 0;
	    
	    // 按前三碼分組
	    Map<String, List<Integer>> groups = new LinkedHashMap<String, List<Integer>>();
	    for (String ip : allIps) {
	        if (ip.contains(":")) continue; // 跳過 IPv6
	        
	        totalCount++; // 累加總數
	        
	        int lastDot = ip.lastIndexOf(".");
	        String prefix = ip.substring(0, lastDot);
	        try {
	            int lastOctet = Integer.parseInt(ip.substring(lastDot + 1));
	            if (!groups.containsKey(prefix)) {
	                groups.put(prefix, new ArrayList<Integer>());
	            }
	            // 避免同一行內重複計算
	            if (!groups.get(prefix).contains(lastOctet)) {
	                groups.get(prefix).add(lastOctet);
	            }
	        } catch (Exception e) {
	            // 防止格式錯誤導致崩潰
	        }
	    }

	    // 3. 壓縮邏輯
	    StringBuilder sb = new StringBuilder();
	    sb.append("# --- IP 壓縮整理結果 ---\n");
	    
	    for (Map.Entry<String, List<Integer>> entry : groups.entrySet()) {
	        String prefix = entry.getKey();
	        List<Integer> suffixList = entry.getValue();
	        Collections.sort(suffixList);

	        sb.append(prefix).append(".");
	        
	        List<String> segments = new ArrayList<String>();
	        int i = 0;
	        while (i < suffixList.size()) {
	            int start = i;
	            while (i + 1 < suffixList.size() && suffixList.get(i + 1) == suffixList.get(i) + 1) {
	                i++;
	            }
	            
	            if (i == start) {
	                segments.add(suffixList.get(start).toString());
	            } else {
	                segments.add(suffixList.get(start) + "-" + suffixList.get(i));
	            }
	            i++;
	        }
	        
	        // 用 / 合併
	        for (int j = 0; j < segments.size(); j++) {
	            sb.append(segments.get(j));
	            if (j < segments.size() - 1) sb.append("/");
	        }
	        sb.append("\n");
	    }

	    // 4. 輸出統計資訊
	    sb.append("\n# --- 統計資訊 ---\n");
	    sb.append("總 IP 數量: ").append(totalCount).append(" 個\n");

	    networkTextArea.setText(sb.toString());
	    networkTextArea.setCaretPosition(0);
	}
		
	/**
	 * * 根據 IP 和 CIDR 計算該網段的第一個可用 IP (Gateway)，例如: 206.119.111.155/26 ->
	 * 206.119.111.129
	 */
	private String calculateFirstAvailableIp(String ipStr, int cidr) {
		try {
			String[] parts = ipStr.split("\\.");
			long ipNum = 0;
			for (int i = 0; i < 4; i++) {
				ipNum = (ipNum << 8) + Integer.parseInt(parts[i]);
			}

			// 產生子網路遮罩 (例如 /26 會產生 26 個 1 接著 6 個 0)
			long mask = (0xFFFFFFFFL << (32 - cidr)) & 0xFFFFFFFFL;

			// 網路位址 = IP & Mask
			long networkAddress = ipNum & mask;

			// 第一個可用 IP = 網路位址 + 1
			long firstIp = networkAddress + 1;

			// 轉回字串格式
			return String.format("%d.%d.%d.%d", (firstIp >> 24) & 0xFF, (firstIp >> 16) & 0xFF, (firstIp >> 8) & 0xFF,
					firstIp & 0xFF);
		} catch (Exception e) {
			return ipStr.substring(0, ipStr.lastIndexOf(".") + 1) + "1"; // 失敗回退
		}
	}

	/** 格式化子網：確保有斜線 */
	private String formatSubnet(String mask) {
		if (mask == null || mask.isEmpty())
			return "/24";
		mask = mask.trim();
		if (!mask.startsWith("/"))
			return "/" + mask;
		return mask;
	}

	//遮罩轉換工具方法
	private String cidrToMask(String cidrStr) {
	    try {
	        // 去除斜線，只取數字部分
	        int prefix = Integer.parseInt(cidrStr.replace("/", "").trim());
	        if (prefix < 0 || prefix > 32) return "255.255.255.0"; // 防錯處理

	        int mask = 0xffffffff << (32 - prefix);
	        return String.format("%d.%d.%d.%d", 
	            (mask >> 24) & 0xff, 
	            (mask >> 16) & 0xff, 
	            (mask >> 8) & 0xff, 
	            mask & 0xff);
	    } catch (Exception e) {
	        return "255.255.255.0"; // 解析失敗時的預設值
	    }
	}
	
	// 創建右鍵功能表
	private JPopupMenu createPopupMenu() {
		JPopupMenu popupMenu = new JPopupMenu();

		// 添加菜單項
		JMenuItem copyItem = new JMenuItem("複製");
		JMenuItem pasteItem = new JMenuItem("貼上");
		JMenuItem cutItem = new JMenuItem("剪下");
		JMenuItem selectAllItem = new JMenuItem("全選");
		JMenuItem undoItem = new JMenuItem("復原");
		JMenuItem redoItem = new JMenuItem("恢復");

		// 綁定功能
		copyItem.addActionListener(e -> textArea.copy());
		pasteItem.addActionListener(e -> textArea.paste());
		cutItem.addActionListener(e -> textArea.cut());
		selectAllItem.addActionListener(e -> textArea.selectAll());
		undoItem.addActionListener(e -> {
			if (undoManager.canUndo())
				undoManager.undo();
		});
		redoItem.addActionListener(e -> {
			if (undoManager.canRedo())
				undoManager.redo();
		});

		// 添加到右鍵菜單
		popupMenu.add(copyItem);
		popupMenu.add(pasteItem);
		popupMenu.add(cutItem);
		popupMenu.addSeparator();
		popupMenu.add(selectAllItem);
		popupMenu.addSeparator();
		popupMenu.add(undoItem);
		popupMenu.add(redoItem);

		return popupMenu;
	}

	// 快捷鍵
	private void setupKeyboardShortcuts(JTextArea targetArea) {
		// 為每個 Area 建立獨立的 UndoManager
		UndoManager localUndo = new UndoManager();
		targetArea.getDocument().addUndoableEditListener(e -> localUndo.addEdit(e.getEdit()));

		InputMap inputMap = targetArea.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = targetArea.getActionMap();

		// 處理 Java 版本相容性的 Mask
		int mask;
		try {
			mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		} catch (Exception e) {
			mask = InputEvent.CTRL_MASK; // 萬一報錯的保險寫法
		}

		// 綁定鍵盤訊號
		inputAreaKeyBind(inputMap, mask);

		// 綁定 Action：復原
		actionMap.put("undo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (localUndo.canUndo())
					localUndo.undo();
			}
		});

		// 綁定 Action：恢復
		actionMap.put("redo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (localUndo.canRedo())
					localUndo.redo();
			}
		});
	}

	// 提取 Mask 綁定邏輯 (支援 Mac/Win)
	private void inputAreaKeyBind(InputMap inputMap, int mask) {
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), "undo");
		if (System.getProperty("os.name").contains("Mac")) {
			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask | InputEvent.SHIFT_DOWN_MASK), "redo");
		} else {
			inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask), "redo");
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

@SuppressWarnings("serial")
class NumberDocument extends PlainDocument {
	public NumberDocument() {
	}

	public void insertString(int var1, String var2, AttributeSet var3) throws BadLocationException {
		if (this.isNumeric(var2)) {
			super.insertString(var1, var2, var3);
		} else {
			Toolkit.getDefaultToolkit().beep();
		}
	}

	private boolean isNumeric(String var1) {
		try {
			Long.valueOf(var1);
			return true;
		} catch (NumberFormatException var3) {
			return false;
		}
	}
}
