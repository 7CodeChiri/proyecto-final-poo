package pe.poo.launcher;

import pe.poo.persistence.ProfileStore;
import pe.poo.persistence.ProfileStore.Profile;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.function.Consumer;

/** Profile selection and the four player-facing CRUD operations. */
final class ProfilesPanel extends JPanel {
    private final ProfileStore store;
    private final JComboBox<Profile> profiles = new JComboBox<>();
    private final DefaultTableModel history = new DefaultTableModel(
        new Object[]{"Nivel", "Enemigos", "Segundos", "Fecha UTC"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JButton play = new JButton("Jugar");
    private final JButton edit = new JButton("Editar alias");
    private final JButton delete = new JButton("Eliminar perfil");

    ProfilesPanel(ProfileStore store, Consumer<Profile> start) throws SQLException {
        super(new BorderLayout(12, 12));
        this.store = store;
        setBackground(new Color(17, 22, 29));
        setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));
        var heading = new JLabel("Untitled Bomberman Game");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 28));
        heading.setForeground(new Color(122, 237, 196));
        heading.setLabelFor(profiles);
        profiles.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                    value instanceof Profile profile ? profile.alias() : "Sin perfil", index, selected, focus);
            }
        });
        var header = new JPanel(new BorderLayout(10, 12));
        header.setOpaque(false);
        header.add(heading, BorderLayout.NORTH);
        header.add(profiles, BorderLayout.CENTER);
        var actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        var create = new JButton("Crear perfil");
        actions.add(create);
        actions.add(edit);
        actions.add(delete);
        actions.add(play);
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(history)), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        create.addActionListener(event -> alias(false));
        edit.addActionListener(event -> alias(true));
        delete.addActionListener(event -> {
            Profile profile = selected();
            if (profile != null && JOptionPane.showConfirmDialog(this,
                    "¿Eliminar el perfil y su historial?", "Eliminar perfil",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                apply(() -> { store.delete(profile.id()); refresh(null); });
            }
        });
        play.addActionListener(event -> {
            if (selected() != null) start.accept(selected());
        });
        profiles.addActionListener(event -> apply(this::refreshHistory));
        refresh(null);
    }

    void refresh(Profile current) throws SQLException {
        var options = store.profiles();
        profiles.setModel(new DefaultComboBoxModel<>(options.toArray(Profile[]::new)));
        if (current != null) options.stream().filter(p -> p.id() == current.id()).findFirst()
            .ifPresent(profiles::setSelectedItem);
        refreshHistory();
    }

    private void alias(boolean rename) {
        Profile profile = selected();
        if (rename && profile == null) return;
        String value = JOptionPane.showInputDialog(this, "Alias del perfil", rename ? profile.alias() : "");
        if (value == null) return;
        apply(() -> {
            if (rename) store.rename(profile.id(), value);
            else store.create(value);
            refresh(profile);
        });
    }

    private void refreshHistory() throws SQLException {
        Profile profile = selected();
        play.setEnabled(profile != null);
        edit.setEnabled(profile != null);
        delete.setEnabled(profile != null);
        history.setRowCount(0);
        if (profile != null) {
            for (var score : store.history(profile.id())) {
                history.addRow(new Object[]{score.level(), score.enemies(), score.seconds(), score.date()});
            }
        }
    }

    private Profile selected() { return (Profile) profiles.getSelectedItem(); }

    private void apply(SqlAction action) {
        try {
            action.run();
        } catch (SQLException | IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Perfiles", JOptionPane.ERROR_MESSAGE);
        }
    }

    @FunctionalInterface
    private interface SqlAction { void run() throws SQLException; }
}
