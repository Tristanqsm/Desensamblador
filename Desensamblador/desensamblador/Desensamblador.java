package desensamblador;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Desensamblador extends JFrame {

    //private JTextArea textArea;
    private JTable instructionTable;
    private JButton uploadButton;
    private JButton BotonAyuda;

    public Desensamblador() {
        // CONFIGURACIÓN DE LA INTERFAZ
        setTitle("Desensamblador Z80 (HEX a Mnemónicos)");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Configuración principal del layout
        setLayout(new BorderLayout());

        // Panel superior para los botones
        JPanel topPanel = new JPanel();
        uploadButton = new JButton("Cargar archivo .HEX");
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFileChooser();
            }
        });

        BotonAyuda = new JButton("Ayuda");
        BotonAyuda.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirPDF("resources/Documentacion.pdf");
            }
        });

        topPanel.add(uploadButton);
        topPanel.add(BotonAyuda);

        // Panel central dividido
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);


        // Tabla de instrucciones a la derecha
        String[] columnNames = {"Dirección", "Código", "Instrucción"};
        Object[][] data = {}; // Datos iniciales vacíos
        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames);
        instructionTable = new JTable(tableModel);

        // Configurar colores para las columnas
        DefaultTableCellRenderer direccionRenderer = new DefaultTableCellRenderer();
        direccionRenderer.setBackground(Color.LIGHT_GRAY);

        DefaultTableCellRenderer codigoRenderer = new DefaultTableCellRenderer();
        codigoRenderer.setBackground(Color.CYAN);

        DefaultTableCellRenderer instruccionRenderer = new DefaultTableCellRenderer();
        instruccionRenderer.setBackground(Color.YELLOW);

        instructionTable.getColumnModel().getColumn(0).setCellRenderer(direccionRenderer);
        instructionTable.getColumnModel().getColumn(1).setCellRenderer(codigoRenderer);
        instructionTable.getColumnModel().getColumn(2).setCellRenderer(instruccionRenderer);

        JScrollPane tableScrollPane = new JScrollPane(instructionTable);

        // Agregar paneles al SplitPane
        //splitPane.setLeftComponent(textScrollPane);
        splitPane.setRightComponent(tableScrollPane);
        splitPane.setDividerLocation(400); // Tamaño inicial

        // Agregar componentes al JFrame
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    // MÉTODO PARA ABRIR EL SELECTOR DE ARCHIVOS
    public void openFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos HEX", "hex"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                LeerArchivoHEX(file);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "ERROR: No se pudo leer el archivo correctamente", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // MÉTODO SIMULADO PARA LEER EL ARCHIVO HEX

    private void LeerArchivoHEX(File file) throws IOException {
        StringBuilder CodigoDesamblado = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String lineas;
        int direccion = 0; // Inicializa dirección en 0
        boolean JRInc = false;
        boolean JPInc = false;
        boolean code4param = false;
        boolean faltanParam = false;
        int opcode = 0;
        int param1 = 0;
        int param2 = 0;
        int paramFaltantes = 0;
        int fullOpcode = 0;
        int subOpcode = 0;
        int i;
        List<Object[]> tablaDatos = new ArrayList<>();
        StringBuilder codigoCompleto = new StringBuilder();
        while ((lineas = reader.readLine()) != null) {
            if (lineas.startsWith(":")) {
                int tamaño = Integer.parseInt(lineas.substring(1, 3), 16);
                int recordType = Integer.parseInt(lineas.substring(7, 9), 16);
                if (recordType == 0x00) {
                    i = 0; // Posición dentro de la línea actual
                    linea: while (i < tamaño) { // Itera sobre los bytes de la línea
                        String mnemonico = "";
                        int bytesConsumidos = 1; // Tamaño base de la instrucción (mínimo 1 byte)
                        if (!JRInc & !JPInc & !code4param & !faltanParam) {
                            opcode = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                            codigoCompleto = new StringBuilder(String.format("%02X", opcode));
                        }
                        //JR
                        if (opcode == 0x18 || opcode == 0x20 || opcode == 0x28 || opcode == 0x30 || opcode == 0x38 || JRInc) { // JR
                            if (i + 1 < tamaño) {
                                int offset = 0;
                                if(JRInc){
                                    offset = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                                    i-=1;
                                }else{
                                    offset = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                }
                                codigoCompleto.append(String.format("%02X", offset));
                                // Calcula el desplazamiento firmado
                                int desplazamiento = (offset < 0x80) ? offset : offset - 0x100;
                                int destino = direccion + desplazamiento + 2; // Suma 2 bytes (tamaño de la instrucción)
                                mnemonico = String.format(Z80Mnemonicos.getMnemonico(opcode), destino);
                                bytesConsumidos = 2; // Opcode + desplazamiento
                                JRInc = false;
                            } else {
                                JRInc = true;
                                break;
                            }
                        }
                        //JP
                        else if (opcode == 0xC2 || opcode == 0xC3 || opcode == 0xCA || opcode == 0xD2 || opcode == 0xDA || opcode == 0xDDE9 || opcode == 0xE2 || opcode == 0xE9 || opcode == 0xEA || opcode == 0xF2 || opcode == 0xFA || opcode == 0xFDE9 || JPInc) { // JP
                            if (i + 2 < tamaño) {
                                if(JPInc & paramFaltantes == 2){
                                    param1 = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                                    param2 = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                    i-=1;
                                }else if(JPInc & paramFaltantes == 1){
                                    param2 = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                                    i-=2;
                                }else{
                                    param1 = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                    param2 = Integer.parseInt(lineas.substring(13 + i * 2, 15 + i * 2), 16);
                                }
                                codigoCompleto.append(String.format(" %02X %02X", param1, param2));
                                int direccionAbsoluta = (param2 << 8) | param1;
                                mnemonico = String.format(Z80Mnemonicos.getMnemonico(opcode), direccionAbsoluta);
                                bytesConsumidos = 3; // Opcode + 2 parámetros
                                JPInc = false;
                            } else if(i + 2 == tamaño){
                                JPInc = true;
                                paramFaltantes = 1;
                                param1 = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                break;
                            }else{
                                JPInc = true;
                                paramFaltantes = 2;
                                break;
                            }
                            //IX Bit Instructions (DDCB)
                        }else if((opcode == 0xDD || opcode == 0xFD) & (Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16) == 0xCB) || subOpcode ==  0xCB){//VERIFICAMOS QUE SEA UN OPCODE QUE INICIA CON DDCB
                            if (i + 3 < tamaño & !code4param) {
                                //System.out.println(i);
                                subOpcode = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                codigoCompleto.append(String.format(" %02X", subOpcode));
                                param1 = Integer.parseInt(lineas.substring(13 + i * 2, 15 + i * 2), 16);
                                fullOpcode = (opcode << 8) | subOpcode;
                                codigoCompleto.append(String.format(" %02X", param1));
                                subOpcode = Integer.parseInt(lineas.substring(15 + i * 2, 17 + i * 2), 16);
                                fullOpcode = (fullOpcode << 8) | subOpcode;
                                codigoCompleto.append(String.format(" %02X", subOpcode));
                                mnemonico = String.format(Z80Mnemonicos.getMnemonico(fullOpcode), param1);
                                bytesConsumidos = 4;
                            }else if (code4param) {
                                if(paramFaltantes == 1){
                                    subOpcode = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                                    fullOpcode = (fullOpcode << 8) | subOpcode;
                                    codigoCompleto.append(String.format(" %02X", subOpcode));
                                    mnemonico = String.format(Z80Mnemonicos.getMnemonico(fullOpcode), param1);
                                    bytesConsumidos = 4;
                                    i-=3;
                                    code4param = false;
                                }else if(paramFaltantes == 2){
                                    param1 = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                                    codigoCompleto.append(String.format(" %02X", param1));
                                    subOpcode = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                    fullOpcode = (fullOpcode << 8) | subOpcode;
                                    codigoCompleto.append(String.format(" %02X", subOpcode));
                                    mnemonico = String.format(Z80Mnemonicos.getMnemonico(fullOpcode), param1);
                                    bytesConsumidos = 4;
                                    i-=2;
                                    code4param = false;
                                }
                            }else{
                                code4param = true;
                                if(i + 3 == tamaño){
                                    paramFaltantes = 1;
                                    subOpcode = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                    codigoCompleto.append(String.format(" %02X", subOpcode));
                                    fullOpcode = (opcode << 8) | subOpcode;
                                    param1 = Integer.parseInt(lineas.substring(13 + i * 2, 15 + i * 2), 16);
                                    codigoCompleto.append(String.format(" %02X", param1));
                                    break linea;
                                }else{
                                    paramFaltantes = 2;
                                    subOpcode = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                    codigoCompleto.append(String.format(" %02X", subOpcode));
                                    fullOpcode = (opcode << 8) | subOpcode;
                                    break linea;
                                }
                            }
                            //OTROS QUE NO SON DDCB
                        }else if (opcode == 0xCB || opcode == 0xDD || opcode == 0xFD || opcode == 0xED || code4param) { // Mnemonico con 2 bytes + parámetros
                            if (i + 1 < tamaño) {
                                if(!code4param){
                                    subOpcode = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                    codigoCompleto.append(String.format(" %02X", subOpcode));
                                    fullOpcode = (opcode << 8) | subOpcode;
                                }
                                if (Z80Mnemonicos.parametros.containsKey(fullOpcode)){
                                    switch (Z80Mnemonicos.parametros.get(fullOpcode)) {
                                        case 1:
                                            if(i + 2 < tamaño){
                                                param1 = code4param ? Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16) : Integer.parseInt(lineas.substring(13 + i * 2, 15 + i * 2), 16);
                                                bytesConsumidos = 3;
                                                if(code4param){i-=2;}
                                                codigoCompleto.append(String.format(" %02X", param1));
                                                mnemonico = String.format(Z80Mnemonicos.getMnemonico(fullOpcode), param1);
                                                code4param = false;
                                            }else{
                                                code4param = true;
                                                break linea;
                                            }
                                            break;
                                        case 2:
                                            if(i + 3 < tamaño){
                                                if(code4param & paramFaltantes == 1){
                                                    param2 = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                                                    bytesConsumidos = 4;
                                                    i-=3;
                                                    codigoCompleto.append(String.format(" %02X %02X", param1, param2));
                                                    if (fullOpcode == 0xDD36 || fullOpcode == 0xFD36) {
                                                        mnemonico = String.format(Z80Mnemonicos.getMnemonico(fullOpcode), param1, param2);
                                                    }else{
                                                        int parametrosUnidos = (param2 << 8) | param1;
                                                        mnemonico = String.format(Z80Mnemonicos.getMnemonico(fullOpcode), parametrosUnidos);

                                                    }
                                                    code4param = false;
                                                    break;
                                                }else if(code4param & paramFaltantes == 2){
                                                    param1 = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                                                    param2 = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                                    //fullOpcode = (fullOpcode << 8) | param;
                                                    bytesConsumidos = 4;
                                                    i-=2;
                                                    codigoCompleto.append(String.format(" %02X %02X", param1, param2));
                                                    if (fullOpcode == 0xDD36 || fullOpcode == 0xFD36) {
                                                        mnemonico = String.format(Z80Mnemonicos.getMnemonico(fullOpcode), param1, param2);
                                                    }else{
                                                        int parametrosUnidos = (param2 << 8) | param1;
                                                        mnemonico = String.format(Z80Mnemonicos.getMnemonico(fullOpcode), parametrosUnidos);

                                                    }
                                                    paramFaltantes = 0;
                                                    code4param = false;
                                                    break;
                                                }else{
                                                    param1 = Integer.parseInt(lineas.substring(13 + i * 2, 15 + i * 2), 16);
                                                    param2 = Integer.parseInt(lineas.substring(15 + i * 2, 17 + i * 2), 16);
                                                    //fullOpcode = (fullOpcode << 8) | param;
                                                    bytesConsumidos = 4;
                                                    codigoCompleto.append(String.format(" %02X %02X", param1, param2));
                                                    if (fullOpcode == 0xDD36 || fullOpcode == 0xFD36) {
                                                        mnemonico = String.format(Z80Mnemonicos.getMnemonico(fullOpcode), param1, param2);
                                                    }else{
                                                        int parametrosUnidos = (param2 << 8) | param1;
                                                        mnemonico = String.format(Z80Mnemonicos.getMnemonico(fullOpcode), parametrosUnidos);

                                                    }
                                                    paramFaltantes = 0;
                                                    break;
                                                }
                                            }else if (i + 2 < tamaño) {
                                                param1 = Integer.parseInt(lineas.substring(13 + i * 2, 15 + i * 2), 16);
                                                code4param  = true;
                                                paramFaltantes = 1;
                                                break linea;
                                            }else{
                                                code4param  = true;
                                                paramFaltantes = 2;
                                                break linea;
                                            }
                                    }

                                }else{
                                    mnemonico = Z80Mnemonicos.getMnemonico(fullOpcode);
                                    bytesConsumidos = 2; // Opcode + subopcode
                                }
                                
                            } else {
                                mnemonico = "ERROR: Código prefijado + parametros incompleto";
                            }
                        }else { // Instrucciones regulares
                            int paramLength = Z80Mnemonicos.getParametro(opcode);
                            if (paramLength == 1){
                                if(i + 1 < tamaño & !faltanParam){
                                    param1 = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                    codigoCompleto.append(String.format(" %02X", param1));
                                    mnemonico = String.format(Z80Mnemonicos.getMnemonico(opcode), param1);
                                    bytesConsumidos = 2; // Opcode + parámetro
                                }else if(paramFaltantes == 1 & faltanParam){
                                    param1 = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                                    codigoCompleto.append(String.format(" %02X", param1));
                                    mnemonico = String.format(Z80Mnemonicos.getMnemonico(opcode), param1);
                                    bytesConsumidos = 2; // Opcode + parámetro
                                    i-=1;
                                    faltanParam = false;
                                    //System.out.println(codigoCompleto);
                                }else{
                                    faltanParam = true;
                                    paramFaltantes = 1;
                                    System.out.println(codigoCompleto);
                                    break linea;
                                }
                            } 
                            else if (paramLength == 2) {
                                if(i + 2 < tamaño & !faltanParam){
                                    param1 = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                    param2 = Integer.parseInt(lineas.substring(13 + i * 2, 15 + i * 2), 16);
                                    codigoCompleto.append(String.format(" %02X %02X", param1, param2));
                                    int fullParam = (param2 << 8) | param1;
                                    mnemonico = String.format(Z80Mnemonicos.getMnemonico(opcode), fullParam);
                                    bytesConsumidos = 3; // Opcode + 2 parámetros
                                }else if(paramFaltantes == 2 & faltanParam){
                                    param1 = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                                    param2 = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                    codigoCompleto.append(String.format(" %02X %02X", param1, param2));
                                    int parametrosUnidos = (param2 << 8) | param1;
                                    mnemonico = String.format(Z80Mnemonicos.getMnemonico(opcode), parametrosUnidos);
                                    bytesConsumidos = 3; // Opcode + 2 parámetros
                                    i-=1;
                                    faltanParam = false;
                                }else if(paramFaltantes == 1 & faltanParam){
                                    param2 = Integer.parseInt(lineas.substring(9 + i * 2, 11 + i * 2), 16);
                                    codigoCompleto.append(String.format(" %02X %02X", param1, param2));
                                    int parametrosUnidos = (param2 << 8) | param1;
                                    mnemonico = String.format(Z80Mnemonicos.getMnemonico(opcode), parametrosUnidos);
                                    bytesConsumidos = 3; // Opcode + 2 parámetros
                                    i-=2;
                                    faltanParam = false;
                                }else{
                                    if(i + 2 == tamaño){
                                        faltanParam = true;
                                        paramFaltantes = 1;
                                        param1 = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                        System.out.println(opcode + "2");
                                        break;
                                    }else{
                                        faltanParam = true;
                                        paramFaltantes = 2;
                                        //param1 = Integer.parseInt(lineas.substring(11 + i * 2, 13 + i * 2), 16);
                                        System.out.println(opcode + "1");
                                        break;
                                    }
                                }
                            } else {
                                mnemonico = Z80Mnemonicos.getMnemonico(opcode);
                            }
                        }
    
                        tablaDatos.add(new Object[]{String.format("%04X", direccion),codigoCompleto.toString(),mnemonico});

                        // Formato de salida con columnas alineadas
                        CodigoDesamblado.append(String.format("%04X: %-15s \t %s%n", direccion, codigoCompleto.toString(), mnemonico));
                        direccion += bytesConsumidos; // Incrementa dirección con los bytes usados
                        i += bytesConsumidos; // Avanza la posición en la línea
                    }
                }
            }
        }
        reader.close();

        // Convertir la lista en un arreglo para la tabla
            Object[][] datos = new Object[tablaDatos.size()][];
            for (int k = 0; k < tablaDatos.size(); k++) {
                datos[k] = (Object[]) tablaDatos.get(k);
            }

        // Mapa para etiquetas y sus direcciones
            Map<Integer, String> etiquetas = new HashMap<>();
            int contadorEtiquetas = 1;

            // Primera pasada (Identificar las etiquetas de los JP y JR)
            for (Object[] fila : tablaDatos) {
                //String direc = (String) fila[0];
                String instruccion = (String) fila[2];

                if (instruccion.startsWith("JP") || instruccion.startsWith("JR")) {
                    // Extraer dirección de salto (último argumento de la instrucción)
                    String[] partes = instruccion.split("\\s+");
                    String destino = partes[partes.length - 1];
                        int direccionDestino = Integer.parseInt(destino, 16); //Pasar a entero
                        if (etiquetas.putIfAbsent(direccionDestino, "eti" + contadorEtiquetas) == null) {
                            contadorEtiquetas++; 
                        }

                }
            }
            
            // Segunda pasada (Poner las etiquetas)
            for (Object[] fila : tablaDatos) {
                String direc = (String) fila[0];
                int direccionDecim = Integer.parseInt(direc, 16);

                // Agregar etiquetas a las direcciones correspondientes
                if (etiquetas.containsKey(direccionDecim)) {
                    String etiqueta = etiquetas.get(direccionDecim);
                    fila[2] = etiqueta + ": " + fila[2]; // Añadir etiqueta al inicio de la instrucción
                }

                // Actualizar mnemónicos con etiquetas en saltos
                String instruccion = (String) fila[2];
                if (instruccion.startsWith("JP") || instruccion.startsWith("JR")) {
                    String[] partes = instruccion.split("\\s+");
                    String destinoStr = partes[partes.length - 1];

                        int direccionDestino = Integer.parseInt(destinoStr, 16);
                        if (etiquetas.containsKey(direccionDestino)) {
                            // Reemplazar la dirección con su etiqueta
                            fila[2] = instruccion.replace(destinoStr, etiquetas.get(direccionDestino));
                        }

                }
            }

            // Actualizar el modelo de la tabla
            DefaultTableModel tableModel = new DefaultTableModel(datos,new String[]{"Dirección", "Código", "Instrucción"});
            instructionTable.setModel(tableModel);

        return;
    }

    // MÉTODO PARA MOSTRAR INFORMACION
    private void abrirPDF(String rutaRecurso) {
        // Leer el archivo desde los recursos
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(rutaRecurso);

        if (inputStream == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el archivo interno.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Crear un archivo temporal para guardar el PDF
            File tempFile = File.createTempFile("archivo", ".pdf");
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }

            // Abrir el archivo PDF con la aplicación predeterminada del sistema
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(tempFile);
            } else {
                JOptionPane.showMessageDialog(this, "Abrir archivos no está soportado en este sistema.", "Error", JOptionPane.ERROR_MESSAGE);
            }

            // Asegurarse de eliminar el archivo temporal al cerrar el programa
            tempFile.deleteOnExit();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al abrir el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // MAIN PARA EJECUTAR LA APLICACIÓN
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Desensamblador().setVisible(true);
            }
        });
    }
}