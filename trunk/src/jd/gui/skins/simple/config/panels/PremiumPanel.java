//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.config.panels;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.skins.simple.components.ChartAPI_Entity;
import jd.gui.skins.simple.components.ChartAPI_PIE;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.config.GUIConfigEntry;
import jd.http.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class PremiumPanel extends JPanel {

    private static final long serialVersionUID = 3275917572262383770L;

    private static final Color ACTIVE = new Color(0x7cd622);
    private static final Color INACTIVE = new Color(0xa40604);

    private PluginForHost host;
    private int accountNum;
    private AccountPanel[] accs;

    private ChartAPI_PIE freeTrafficChart = new ChartAPI_PIE("", 450, 60, this.getBackground());

    public PremiumPanel(GUIConfigEntry gce) {
        host = (PluginForHost) gce.getConfigEntry().getActionListener();
        accountNum = gce.getConfigEntry().getEnd();
        setLayout(new MigLayout("ins 5", "[right, pref!]10[100:pref, grow,fill]0[right][100:pref, grow,fill]"));
        createPanel();
    }

    /**
     * Gibt alle aktuellen Accounts zurück
     * 
     * @return
     */
    public ArrayList<Account> getAccounts() {
        ArrayList<Account> accounts = new ArrayList<Account>();
        for (AccountPanel acc : accs) {
            accounts.add(acc.getAccount());
        }
        return accounts;
    }

    /**
     * List ist immer eine ArrayList<Account> mit Daten aus der config
     * 
     * @param list
     */
    @SuppressWarnings("unchecked")
    public void setAccounts(Object list) {
        ArrayList<Account> accounts = (ArrayList<Account>) list;
        for (int i = 0; i < accountNum; i++) {
            if (i >= accounts.size()) break;
            accs[i].setAccount(accounts.get(i));
        }
        createDataset();
    }

    private void createDataset() {
        new ChartRefresh().start();
    }

    private void createPanel() {
        accs = new AccountPanel[accountNum];

        JPanel panel = this;
        JTabbedPane tab = new JTabbedPane();
        for (int i = 0; i < accountNum; i++) {
            if (i % 5 == 0 && accountNum > 5) {
                tab.add(panel = new JPanel());
                panel.setLayout(new MigLayout("ins 5", "[right, pref!]10[100:pref, grow,fill]0[right][100:pref, grow,fill]"));
            }
            panel.add(accs[i] = new AccountPanel(i + 1), "span");
        }

        if (accountNum > 5) {
            int i;
            for (i = 0; i < tab.getTabCount() - 1; i++) {
                tab.setTitleAt(i, JDLocale.L("plugins.menu.accounts", "Accounts") + ": " + (i * 5 + 1) + " - " + ((i + 1) * 5));
            }
            tab.setTitleAt(i, JDLocale.L("plugins.menu.accounts", "Accounts") + ": " + ((i * 5 + 1 == accountNum) ? accountNum : (i * 5 + 1) + " - " + accountNum));
            this.add(tab, "span");
        }

        String premiumUrl = host.getBuyPremiumUrl();
        if (premiumUrl != null) {
            try {
                this.add(new JLinkButton(JDLocale.L("plugins.premium.premiumbutton", "Get Premium Account"), new URL("http://jdownloader.org/r.php?u=" + Encoding.urlEncode(premiumUrl))), "span, alignright");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        this.add(freeTrafficChart, "spanx, spany");
    }

    private class AccountPanel extends JPanel implements ChangeListener, ActionListener, FocusListener {

        private static final long serialVersionUID = 5577685065338731763L;

        private JCheckBox chkEnable;
        private JLabel lblUsername;
        private JLabel lblPassword;
        private JTextField txtUsername;
        private JDPasswordField txtPassword;
        private JTextField txtStatus;
        private JButton btnCheck;
        private JButton btnDelete;

        public AccountPanel(int nr) {
            setLayout(new MigLayout("ins 5", "[right, pref!]10[100:pref, grow,fill]0[right][100:pref, grow,fill]"));
            createPanel(nr);
        }

        public void setAccount(Account account) {
            boolean sel = account.isEnabled();
            txtUsername.setText(account.getUser());
            txtPassword.setText(account.getPass());
            txtStatus.setText(account.getStatus());
            chkEnable.setSelected(sel);
            lblUsername.setEnabled(sel);
            lblPassword.setEnabled(sel);
            txtUsername.setEnabled(sel);
            txtPassword.setEnabled(sel);
            txtStatus.setEnabled(sel);
            btnCheck.setEnabled(sel);
        }

        public Account getAccount() {
            Account a = new Account(txtUsername.getText(), new String(txtPassword.getPassword()));
            a.setEnabled(chkEnable.isSelected());
            return a;
        }

        public void createPanel(int nr) {
            add(chkEnable = new JCheckBox(JDLocale.LF("plugins.config.premium.accountnum", "<html><b>Premium Account #%s</b></html>", nr)), "alignleft");
            chkEnable.setForeground(INACTIVE);
            chkEnable.setSelected(true);
            chkEnable.addChangeListener(this);

            add(btnCheck = new JButton(JDLocale.L("plugins.config.premium.test", "Get Status")), "w pref:pref:pref, split 2");
            btnCheck.addActionListener(this);

            add(btnDelete = new JButton(JDUtilities.getScaledImageIcon(JDTheme.V("gui.images.exit"), -1, 14)));
            btnDelete.addActionListener(this);

            add(new JSeparator(), "w 30:push, growx, pushx");
            add(txtStatus = new JTextField(""), "spanx, pushx, growx");
            txtStatus.setEditable(false);

            add(lblUsername = new JLabel(JDLocale.L("plugins.config.premium.user", "Premium User")), "gaptop 8");
            add(txtUsername = new JTextField(""));
            txtUsername.addFocusListener(this);

            add(lblPassword = new JLabel(JDLocale.L("plugins.config.premium.password", "Password")), "gapleft 15");
            add(txtPassword = new JDPasswordField(), "span, gapbottom 10:10:push");
            txtPassword.addFocusListener(this);

            chkEnable.setSelected(false);
        }

        public void stateChanged(ChangeEvent e) {
            boolean sel = chkEnable.isSelected();
            chkEnable.setForeground((sel) ? ACTIVE : INACTIVE);
            txtPassword.setEnabled(sel);
            txtUsername.setEnabled(sel);
            txtStatus.setEnabled(sel);
            btnCheck.setEnabled(sel);
            lblPassword.setEnabled(sel);
            lblUsername.setEnabled(sel);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnCheck) {
                JDUtilities.getGUI().showAccountInformation(host, getAccount());
            } else if (e.getSource() == btnDelete) {
                txtUsername.setText("");
                txtPassword.setText("");
                txtStatus.setText("");
                chkEnable.setSelected(false);
                createDataset();
            }
        }

        public void focusGained(FocusEvent e) {
            ((JTextField) e.getSource()).selectAll();
        }

        public void focusLost(FocusEvent e) {
            createDataset();
        }
    }

    private class ChartRefresh extends Thread {
        @Override
        public void run() {
            Long collectTraffic = new Long(0);
            freeTrafficChart.clear();
            int accCounter = 0;
            for (Account acc : getAccounts()) {
                if (acc.getUser().length() > 0 && acc.getPass().length() > 0) {
                    try {
                        accCounter++;
                        AccountInfo ai = host.getAccountInformation(acc);
                        Long tleft = new Long(ai.getTrafficLeft());
                        if (tleft >= 0 && ai.isExpired() == false) {
                            freeTrafficChart.addEntity(new ChartAPI_Entity(acc.getUser() + " [" + (Math.round(tleft.floatValue() / 1024 / 1024 / 1024 * 100) / 100.0) + " GB]", tleft, new Color(50, 255 - ((255 / (accountNum + 1)) * accCounter), 50)));
                            long rest = ai.getTrafficMax() - tleft;
                            if (rest > 0) collectTraffic = collectTraffic + rest;
                        }
                    } catch (Exception e) {
                        JDUtilities.getLogger().finest("Not able to load Traffic-Limit for ChartAPI");
                    }
                }
            }

            if (collectTraffic > 0) freeTrafficChart.addEntity(new ChartAPI_Entity(JDLocale.L("plugins.config.premium.chartapi.maxTraffic", "Max. Traffic to collect") + " [" + Math.round(((collectTraffic.floatValue() / 1024 / 1024 / 1024) * 100) / 100.0) + " GB]", collectTraffic, new Color(150, 150, 150)));
            freeTrafficChart.fetchImage();
        }
    }

    private class JDPasswordField extends JPasswordField implements ClipboardOwner {

        private static final long serialVersionUID = -7981118302661369727L;

        public JDPasswordField() {
            super();
        }

        @Override
        public void cut() {
            StringSelection stringSelection = new StringSelection(String.valueOf(this.getSelectedText()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);

            String text = String.valueOf(this.getPassword());
            int position = this.getSelectionStart();
            String s1 = text.substring(0, position);
            String s2 = text.substring(this.getSelectionEnd(), text.length());
            this.setText(s1 + s2);

            this.setSelectionStart(position);
            this.setSelectionEnd(position);
        }

        @Override
        public void copy() {
            StringSelection stringSelection = new StringSelection(String.valueOf(this.getSelectedText()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }

        public void lostOwnership(Clipboard arg0, Transferable arg1) {
        }

    }

}