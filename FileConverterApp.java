import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

public class FileConverterApp extends JFrame {
    private JButton selectFilesButton;
    private JButton selectOutputDirButton;
    private JComboBox<String> conversionTypeComboBox;
    private JButton startButton;
    private JButton cancelButton;
    private JProgressBar overallProgressBar;
    private JPanel fileProgressPanel;
    private JLabel statusLabel;

    private ExecutorService executorService;
    private List<File> selectedFiles;
    private File outputDirectory;
    private List<SwingWorker<Void, Integer>> workers;

    public FileConverterApp() {
        super("File Converter");
        initComponents();
        initExecutorService();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        selectFilesButton = new JButton("Select Files");
        selectOutputDirButton = new JButton("Select Output Directory");
        conversionTypeComboBox = new JComboBox<>(new String[]{"PDF to Docx", "Image Resize"});
        startButton = new JButton("Start Conversion");
        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(false);

        topPanel.add(selectFilesButton);
        topPanel.add(selectOutputDirButton);
        topPanel.add(conversionTypeComboBox);
        topPanel.add(startButton);
        topPanel.add(cancelButton);

        add(topPanel, BorderLayout.NORTH);

        overallProgressBar = new JProgressBar(0, 100);
        add(overallProgressBar, BorderLayout.SOUTH);

        fileProgressPanel = new JPanel();
        fileProgressPanel.setLayout(new BoxLayout(fileProgressPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(fileProgressPanel);
        add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Ready");
        add(statusLabel, BorderLayout.SOUTH);

        attachListeners();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
    }

    private void initExecutorService() {
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    private void attachListeners() {
        selectFilesButton.addActionListener(e -> selectFiles());
        selectOutputDirButton.addActionListener(e -> selectOutputDirectory());
        startButton.addActionListener(e -> startConversion());
        cancelButton.addActionListener(e -> cancelConversion());
    }

    private void selectFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFiles = List.of(fileChooser.getSelectedFiles());
            statusLabel.setText(selectedFiles.size() + " files selected");
        }
    }

    private void selectOutputDirectory() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = dirChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            outputDirectory = dirChooser.getSelectedFile();
            statusLabel.setText("Output directory: " + outputDirectory.getAbsolutePath());
        }
    }

    private void startConversion() {
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select files first.");
            return;
        }
        if (outputDirectory == null) {
            JOptionPane.showMessageDialog(this, "Please select an output directory.");
            return;
        }

        fileProgressPanel.removeAll();
        workers = new CopyOnWriteArrayList<>();
        overallProgressBar.setValue(0);
        startButton.setEnabled(false);
        cancelButton.setEnabled(true);

        String conversionType = (String) conversionTypeComboBox.getSelectedItem();

        for (File file : selectedFiles) {
            FileConversionWorker worker = new FileConversionWorker(file, conversionType);
            workers.add(worker);
            worker.execute();
        }

        new OverallProgressTracker().execute();
    }

    private void cancelConversion() {
        for (SwingWorker<Void, Integer> worker : workers) {
            worker.cancel(true);
        }
        executorService.shutdownNow();
        initExecutorService();
        cancelButton.setEnabled(false);
        startButton.setEnabled(true);
        statusLabel.setText("Conversion cancelled");
    }

    private class FileConversionWorker extends SwingWorker<Void, Integer> {
        private final File file;
        private final String conversionType;
        private final JProgressBar fileProgressBar;
        private final JLabel fileLabel;

        public FileConversionWorker(File file, String conversionType) {
            this.file = file;
            this.conversionType = conversionType;
            this.fileProgressBar = new JProgressBar(0, 100);
            this.fileLabel = new JLabel(file.getName());

            JPanel filePanel = new JPanel(new BorderLayout());
            filePanel.add(fileLabel, BorderLayout.NORTH);
            filePanel.add(fileProgressBar, BorderLayout.CENTER);
            fileProgressPanel.add(filePanel);
            fileProgressPanel.revalidate();
        }

        @Override
        protected Void doInBackground() throws Exception {
            File outputFile = new File(outputDirectory, getOutputFileName(file.getName(), conversionType));

            if (conversionType.equals("PDF to Docx")) {
                convertPdfToDoc(file, outputFile);
            } else if (conversionType.equals("Image Resize")) {
                resizeImage(file, outputFile);
            }

            return null;
        }

        private String getOutputFileName(String inputFileName, String conversionType) {
            String baseName = inputFileName.substring(0, inputFileName.lastIndexOf('.'));
            if (conversionType.equals("PDF to Docx")) {
                return baseName + ".doc";
            } else if (conversionType.equals("Image Resize")) {
                return baseName + "_resized" + inputFileName.substring(inputFileName.lastIndexOf('.'));
            }
            return inputFileName + "_converted";
        }

        private void convertPdfToDoc(File input, File output) throws IOException {
            try (BufferedReader reader = new BufferedReader(new FileReader(input));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
                // Write a simple header to make it a basic .doc file
                writer.write("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">");
                writer.write("<meta name=\"Generator\" content=\"Microsoft Word 15 (filtered)\">");
                writer.write("<style><!-- /* Font Definitions */ @font-face {font-family:\"Cambria Math\"; panose-1:2 4 5 3 5 4 6 3 2 4;} ");
                writer.write("@font-face {font-family:Calibri; panose-1:2 15 5 2 2 2 4 3 2 4;} /* Style Definitions */ ");
                writer.write("p.MsoNormal, li.MsoNormal, div.MsoNormal {margin:0in; font-size:11.0pt; font-family:\"Calibri\",sans-serif;} ");
                writer.write(".MsoChpDefault {font-size:10.0pt; font-family:\"Calibri\",sans-serif;} @page WordSection1 {margin:1.0in 1.0in 1.0in 1.0in;} ");
                writer.write("div.WordSection1 {page:WordSection1;} --></style></head><body lang=\"EN-US\" style=\"word-wrap:break-word\">");
                writer.write("<div class=\"WordSection1\"><p class=\"MsoNormal\">");

                String line;
                int linesProcessed = 0;
                while ((line = reader.readLine()) != null && !isCancelled()) {
                    // Escape any HTML special characters
                    line = line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                    writer.write(line);
                    writer.write("<br>");
                    linesProcessed++;
                    publish(linesProcessed * 100 / 1000); // Assuming 1000 lines total
                }

                // Close the HTML tags
                writer.write("</p></div></body></html>");
            }
        }

        private void resizeImage(File input, File output) throws IOException {
            BufferedImage originalImage = ImageIO.read(input);
            int newWidth = originalImage.getWidth() / 2;
            int newHeight = originalImage.getHeight() / 2;

            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g.dispose();

            String format = output.getName().substring(output.getName().lastIndexOf('.') + 1);
            ImageIO.write(resizedImage, format, output);
            publish(100);
        }

        @Override
        protected void process(List<Integer> chunks) {
            int latestProgress = chunks.get(chunks.size() - 1);
            fileProgressBar.setValue(latestProgress);
            fileLabel.setText(file.getName() + " - " + conversionType + " - " + latestProgress + "%");
        }

        @Override
        protected void done() {
            try {
                get();
                fileLabel.setText(file.getName() + " - Conversion completed");
            } catch (InterruptedException | ExecutionException e) {
                fileLabel.setText(file.getName() + " - Conversion failed: " + e.getMessage());
            } catch (CancellationException e) {
                fileLabel.setText(file.getName() + " - Conversion cancelled");
            }
        }
    }

    private class OverallProgressTracker extends SwingWorker<Void, Integer> {
        @Override
        protected Void doInBackground() throws Exception {
            while (!isCancelled() && !allWorkersFinished()) {
                Thread.sleep(100);
                publish(calculateOverallProgress());
            }
            return null;
        }

        @Override
        protected void process(List<Integer> chunks) {
            int latestProgress = chunks.get(chunks.size() - 1);
            overallProgressBar.setValue(latestProgress);
            statusLabel.setText("Overall progress: " + latestProgress + "%");
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                JOptionPane.showMessageDialog(FileConverterApp.this, "All conversions completed!");
            }
            startButton.setEnabled(true);
            cancelButton.setEnabled(false);
        }

        private boolean allWorkersFinished() {
            return workers.stream().allMatch(SwingWorker::isDone);
        }

        private int calculateOverallProgress() {
            int totalProgress = workers.stream().mapToInt(worker -> {
                try {
                    return ((FileConversionWorker) worker).fileProgressBar.getValue();
                } catch (Exception e) {
                    return 0;
                }
            }).sum();
            return totalProgress / workers.size();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FileConverterApp().setVisible(true));
    }
}