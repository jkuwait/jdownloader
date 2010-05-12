//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.settings.panels.premium;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JScrollPane;
import javax.swing.Timer;

import jd.HostPluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.dialog.AccountDialog;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.views.ViewToolbar;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.settings.JDLabelListRenderer;
import jd.nutils.JDFlags;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class Premium extends ConfigPanel implements ActionListener, AccountControllerListener {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.premium.Premium.";

    private static final long serialVersionUID = -7685744533817989161L;
    private PremiumTable internalTable;
    private JScrollPane scrollPane;
    private Timer updateAsync;

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "title", "Premium");
    }

    public static String getIconKey() {
        return "gui.images.premium";
    }

    public Premium() {
        super();

        init();
    }

    @Override
    protected ConfigContainer setupContainer() {
        internalTable = new PremiumTable(this);

        scrollPane = new JScrollPane(internalTable);

        updateAsync = new Timer(250, this);
        updateAsync.setInitialDelay(250);
        updateAsync.setRepeats(false);

        AccountController.getInstance().addListener(this);
        initActions();

        ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(getTitle(), getIconKey()));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, new ViewToolbar("action.premiumview.addacc", "action.premiumview.removeacc", "action.premium.buy"), ""));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, scrollPane, "growy, pushy"));

        container.setGroup(new ConfigGroup(JDL.L(JDL_PREFIX + "settings", "Advanced Settings"), getIconKey()));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, AccountController.getInstance(), AccountController.PROPERTY_ACCOUNT_SELECTION, JDL.L(JDL_PREFIX + "accountSelection", "Always select the premium account with the most traffic left for downloading")));
        return container;
    }

    @Override
    public PropertyType hasChanges() {
        return PropertyType.NORMAL;
    }

    private void initActions() {
        new ThreadedAction("action.premiumview.addacc", "gui.images.newlogins") {

            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                internalTable.editingStopped(null);
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        if (e.getSource() instanceof PluginForHost) {
                            AccountDialog.showDialog((PluginForHost) e.getSource());
                        } else {
                            AccountDialog.showDialog(null);
                        }

                        return null;
                    }
                }.start();
            }
        };
        new ThreadedAction("action.premiumview.removeacc", "gui.images.delete") {

            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                ArrayList<Account> accs = internalTable.getAllSelectedAccounts();
                internalTable.editingStopped(null);
                if (accs.size() == 0) return;
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, JDL.L("action.premiumview.removeacc.ask", "Remove selected?") + " (" + JDL.LF("action.premiumview.removeacc.accs", "%s Account(s)", accs.size()) + ")"), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                    for (Account acc : accs) {
                        AccountController.getInstance().removeAccount((String) null, acc);
                    }
                }
            }
        };

        new ThreadedAction("action.premium.buy", "gui.images.buy") {

            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                internalTable.editingStopped(null);
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {

                        ArrayList<HostPluginWrapper> plugins = JDUtilities.getPremiumPluginsForHost();
                        Collections.sort(plugins, new Comparator<HostPluginWrapper>() {
                            public int compare(HostPluginWrapper a, HostPluginWrapper b) {
                                return a.getHost().compareToIgnoreCase(b.getHost());
                            }
                        });
                        HostPluginWrapper[] data = plugins.toArray(new HostPluginWrapper[plugins.size()]);
                        int selection = UserIO.getInstance().requestComboDialog(0, JDL.L(JDL_PREFIX + "buy.title", "Buy Premium"), JDL.L(JDL_PREFIX + "buy.message", "Which hoster are you interested in?"), data, 0, null, JDL.L(JDL_PREFIX + "continue", "Continue"), null, new JDLabelListRenderer());

                        try {
                            JLink.openURL(data[selection].getPlugin().getBuyPremiumUrl());
                        } catch (Exception ex) {
                        }

                        return null;
                    }

                }.start();

            }
        };

    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    @Override
    public void onHide() {
        super.onHide();
        updateAsync.stop();
        AccountController.getInstance().removeListener(this);
    }

    public void fireTableChanged() {
        try {
            internalTable.fireTableChanged();
        } catch (Exception e) {
            logger.severe("TreeTable Exception, complete refresh!");
            updateAsync.restart();
        }
    }

    @Override
    public void onShow() {
        super.onShow();
        AccountController.getInstance().addListener(this);
        fireTableChanged();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == updateAsync) {
            fireTableChanged();
        }
    }

    public void onAccountControllerEvent(AccountControllerEvent event) {
        switch (event.getID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
        case AccountControllerEvent.ACCOUNT_REMOVED:
        case AccountControllerEvent.ACCOUNT_UPDATE:
        case AccountControllerEvent.ACCOUNT_EXPIRED:
        case AccountControllerEvent.ACCOUNT_INVALID:
            updateAsync.restart();
            break;
        default:
            break;
        }
    }

    public void setSelectedAccount(Account param) {
        int row = ((PremiumJTableModel) internalTable.getModel()).getRowforObject(param);
        this.internalTable.getSelectionModel().setSelectionInterval(row, row);
        this.internalTable.scrollRowToVisible(row);
    }

}
