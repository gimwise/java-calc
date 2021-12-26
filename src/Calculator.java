import javax.swing.*;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Stack;

class ImagePanel extends JPanel {
    Image image;
    Toolkit toolkit = getToolkit();

    public ImagePanel(LayoutManager layout){
        super(layout);
    }

    public void paint(Graphics g){
        Dimension d = getSize();

        if(image != null){
            g.drawImage(image, 0,0,d.width, d.height, null);
        }
    }

    void setPath(String path){
        image = toolkit.getImage(path);
    }

}

class LoadImage implements ActionListener{
    JTextField text;
    ImagePanel imagePanel;

    public LoadImage(JTextField text, ImagePanel imagePanel){
        this.text = text;
        this.imagePanel = imagePanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        imagePanel.setPath(text.getText());
        imagePanel.repaint();
    }
}

public class Calculator extends JFrame implements ActionListener {

    private JTabbedPane jtp;

    private JPanel calPan;
    private JLabel guide;

    // controlPan 위에 text와 upload가 올라감
    private JPanel controlPan;
    private JTextField text;
    private JButton upload;

    // resultPan 위에 result1과 result2가 올라감
    private ImagePanel resultPan;
    private JLabel result1; // 이때까지 식
    private JLabel result2; // 현재 계산 값 또는 입력 값

    // btnPan 위에 btn들 올라감
    private JPanel btnPan;
    private JButton btn[] = new JButton[25];
    private String btnName[] = {
            "←", "CE", "C", "%","x!",
            "7", "8", "9", "/", "++",
            "4", "5", "6", "*", "--",
            "1", "2","3", "-", "sqrt",
            "0", ".", "1/x", "+", "=" };

    // 각 연산이 실행되는지 확인하는 flag
    private boolean dot = false;
    private boolean isResult = false; // 현재 값이 연산되어 나온 결과값인가

    private String oper = new String(); // 연산자 string 저장
    private String strTemp; // result2 결과
    private StringBuffer strBuf = new StringBuffer(); // result1 수정 및 저장

    public Calculator(){
        setTitle("자바 텀프로젝트 계산기 by 김지혜");
        setLayout(new BorderLayout(5,5));
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        init();

        setSize(400,450);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // init() - 각 Panel 합치기
    private void init(){
        jtp = new JTabbedPane();
        calPan = new JPanel(new BorderLayout());

        controlPan = new JPanel(new GridLayout(1,2));
        resultPan = new ImagePanel(new BorderLayout(5,5));
        btnPan = new JPanel(new GridLayout(5,5,2,2));

        text = new JTextField(30);
        upload = new JButton("이미지 로드");
        upload.addActionListener(new LoadImage(text, resultPan));

        controlPan.add(text);
        controlPan.add(upload);

        calPan.add("North", controlPan);

        result1 = new JLabel();
        result2 = new JLabel();

        result1.setText(" ");
        result1.setForeground(Color.BLUE);
        result1.setHorizontalAlignment(JTextField.RIGHT);
        result1.setOpaque(true);

        result2.setHorizontalAlignment(JTextField.RIGHT);
        result2.setFont(new Font(null, 0, 20));
        result2.setText("0");
        result2.setOpaque(true);

        resultPan.add("North", result1);
        resultPan.add("South", result2);

        calPan.add("Center", resultPan);

        for (int i = 0; i < btn.length; i++) {
            btn[i] = new JButton(btnName[i]);

            btn[i].addActionListener(this);
            btn[i].setFocusable(false);
            btnPan.add(btn[i]);
        }

        calPan.add("South",btnPan);

        guide = new JLabel("<html><body style='text-align:center;'>김지혜 계산기 사용법<br /><br />- 주소를 입력하고 이미지 로드 버튼을 2번 누르면 자신이 원하는 이미지로 결과창을 꾸밀 수 있습니다.<br />- 이 계산기는 피연사자에 음수가 지원되지 않습니다.<br />- 연산자의 우선순위는 고려됩니다.<br />- 증감연산자 ’--’는 0까지만 지원되고 음수는 지원하지 않습니다.<br />- '%' 연산자는 피연산자를 실수 백분율로 나타냈을 때의 값을 보여줍니다.<br />- '='를 누를 때 티모 웃음 소리에 주의하세요!</body></html>", JLabel.CENTER);

        jtp.addTab("계산기", calPan);
        jtp.addTab("설명서", guide);

        add(jtp);
    }

    private void clearFlag(){
        dot = false;
        strBuf = new StringBuffer();
    }

    private void clearAll(){
        result1.setText(" ");
        result2.setText("0");

        oper = new String();
        isResult = false;

        clearFlag();
    }

    private void operation(String oper){
        strTemp = result1.getText() + result2.getText();

        result1.setText(strTemp + oper);

    }

    //연산자 우선순위 계산 (흔히 알고 있는 그 순서입니다)
    static int opOrder(char op) {
        switch(op) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
                return 2;
            default:
                return -1;
        }
    }

    // 엔터 눌렸을 때 실행되는 연산
    private double calc(String input){
        char operation[] = {'+', '-', '*', '/'};

        ArrayList<String> postfix = new ArrayList<>(); // 후위표기법 list
        Stack<Character> operStack = new Stack<>(); // 연산자 Stack
        Stack<String> calcStack = new Stack<>(); // 후위 표기법 계산을 위한 Stack
        String val = ""; // 피연산자 저장할 변수

        // -로 시작하는 경우 앞에 0 붙이기 (이 계산기에서는 피연산자가 양수여야함)
        if(input.charAt(0) == '-') {
            input = "0" + input;
        }

        // 후위 표기법으로 변경하는 과정
        /* <후위 표기법으로 변경하는 방법> (조행래 교수님 자료구조 강의자료 참고해서 작성)
        * 1. 피연산자는 operStack에 넣지 않고 val에 저장하고 연산자가 나올 때 val을 postifx에 저장
        * 2. 연산자 스택이 비어있으면 push
        * 3. 연산자 스택이 비어있지 않으면 스택에 있는 연산자와의 우선순위를 비교해
        *   스택에 있는 연산자의 우선순위가 같거나 크다면 스택에 있는 연산자를 pop하여 postfix에 저장하고
        *   현재 연산자를 operStack에 push
        *   but, 현재 연산자가 우선순위가 크다면 현재 연산자를 operStack에 push
        * 4. 수식이 끝까지 수행되면 마지막 val에 있는 피연산자를 postfix에 저장하고 operStack이 empty가 될 때까지 pop해서 postfix에 저장
        * */
        for(int i = 0; i < input.length(); i++) {
            boolean checkOp = false;
            for(int j = 0; j < operation.length; j++) {
                if(input.charAt(i) == operation[j]) {

                    checkOp = true;

                    if(!val.equals("")) {
                        postfix.add(val);
                        val = "";
                    }
                    if(operStack.isEmpty()) {
                        operStack.push(operation[j]);
                    }
                    else {
                        if(opOrder(operStack.peek()) < opOrder(operation[j])) {
                            operStack.push(operation[j]);
                        }else {
                            postfix.add(operStack.pop().toString());
                            operStack.push(operation[j]);
                        }
                    }
                }
            }

            if(!checkOp) {
                val += input.charAt(i);
            }

        }

        // 남은 숫자 처리
        if(!val.equals("")) {
            postfix.add(val);
        }

        // 남은 연산자 처리
        while(!operStack.isEmpty()) {
            postfix.add(operStack.pop().toString());
        }

        /*
        * <후위표기법 계산>
        * 1. postfix의 값을 calcStack에 저장
        * 2. calcStack의 top 값이 연산자면 pop하여 계산
        * 3. 마지막에 top에 결과가 저장
        */
        for(int i = 0; i < postfix.size(); i++) {
            calcStack.push(postfix.get(i));
            for(int j = 0; j < operation.length; j++) {
                if(postfix.get(i).charAt(0) == operation[j]) {
                    calcStack.pop();
                    Double n2 = Double.parseDouble(calcStack.pop());
                    String re = "";

                    Double n1 = Double.parseDouble(calcStack.pop());
                    if(operation[j] == '+') {
                        re = Double.toString(n1 + n2);
                    }else if(operation[j] == '-') {
                        re = Double.toString(n1 - n2);
                    }else if(operation[j] == '*') {
                        re = Double.toString(n1 * n2);
                    }else if(operation[j] == '/') {
                        re = Double.toString(n1 / n2);
                    }
                    calcStack.push(re);
                }
            }
        }

        // 결과값 저장
        Double result = Double.parseDouble(calcStack.pop());
//        System.out.println(result);

        return result;
    }

    // '=' 눌렀을 때 실행
    private void enterOperation(){
        double cal = 0;
        strTemp = result1.getText() + result2.getText();
        cal = calc(strTemp);
        result1.setText(" ");

        if(cal ==(long)cal){
            result2.setText("" + (long)cal);
        }
        else{
            result2.setText("" + cal);
        }

        isResult = true;
    }
    // '%' 눌렀을 때 실행
    private void perOperation(){
        String output = "";
        double doubleTemp = 0.0;

        doubleTemp = Double.parseDouble(result2.getText());

        double d = doubleTemp / 100;

        output += d;

        result2.setText(output);

        clearFlag();
    }
    // 소수점 늘렀을 때 실행
    private void dotOperation(String s){
        if (!dot) { // 소수점이 이때까지 없었다면
            if (result2.getText().equals("0")) { // 현재 result2 창의 값이 0이라면
                if (strBuf.toString().equals("0"))
                    strBuf.append(s); // 소수점 붙여라
                else
                    strBuf.append("0" + s);
            } else {
                if (strBuf.toString().equals(""))
                    strBuf.append("0" + s);
                else
                    strBuf.append(s);
            }
            result2.setText(strBuf.toString());
            dot = true;
        }
    }
    // 백스페이스 실행
    private void backOperation(){
        if (!isResult) { // 결과값이면 백스페이스 안됨 -> CE, C 사용하세요
            if (strBuf.length() > 0) {
                if (strBuf.charAt(strBuf.length() - 1) == '.') // 소수점을 만나면 dot = false
                    dot = false;
                strBuf.deleteCharAt(strBuf.length() - 1);
            }

            if (strBuf.length() == 0)
                result2.setText("0");
            else
                result2.setText(strBuf.toString());
        }
    }
    // 숫자 버튼 눌렀을 때 실행
    private void numOperation(String s){
        if (s.equals("0") && result2.getText().equals("0")) {
            result2.setText("0"); // 0 연타 안되게
        }
        else { // 숫자 16자리(소수점 포함) 찍을 수 있음
            if (strBuf.length() < 16) {
                strBuf.append(s);
                result2.setText(strBuf.toString());
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        String btnEvent = ((JButton) e.getSource()).getText();

        // 백스페이스
        if (btnEvent.equals("←")) {
            backOperation();
        }

        // CE - 현재 입력값만 지우기
        else if (btnEvent.equals("CE")) {
            result2.setText("0");
            clearFlag();
        }

        // C - 모든 값 지우기
        else if (btnEvent.equals("C")) {
            clearAll();
        }

        // 제곱근 연산
        else if (btnEvent.equals("sqrt")) {
            String output = "";

            double doubleTmp = 0.0;

            doubleTmp = Double.parseDouble(result2.getText());

            // 우리 계산기는 양수만 제곱근을 계산합니다.
            if (doubleTmp < 0.0)
                output += "잘못된 입력입니다.";

            else {
                double d = Math.sqrt(doubleTmp);

                if (d == (long) d) {
                    output += (long) d;

                } else {
                    output += d;
                }
            }

            result2.setText(output);
            clearFlag();
        }

        // 더하기
        else if (btnEvent.equals("+")) {
            operation("+");
            clearFlag();
        }

        // 빼기
        else if (btnEvent.equals("-")) {
            operation("-");
            clearFlag();
        }

        // 나누기
        else if (btnEvent.equals("/")) {
            operation("/");
            clearFlag();
        }

        // 곱하기
        else if (btnEvent.equals("*")) {
            operation("*");
            clearFlag();
        }

        // 퍼센트 double로 표현하기
        else if (btnEvent.equals("%")) {
            perOperation();
        }

        // 역수 구하기
        else if (btnEvent.equals("1/x")) {
            String output = "";
            double doubleTmp = 0.0;

            doubleTmp = Double.parseDouble(result2.getText());
            double d = Math.pow(doubleTmp, -1); // Math 이용해 -1승 구하기

            if (d == (long) d) {
                output += (long) d;

            } else {
                output += d;
            }

            result2.setText(output); // 결과 저장
            clearFlag();
//			System.out.println("1/x : " + cal1 + " " + cal2);
        }

        // 소수점 찍기
        else if (btnEvent.equals(".")) {
            dotOperation(btnEvent);
        }

        // 팩토리얼
        else if (btnEvent.equals("x!")) {
            boolean doubleFlag = false;
            String output = "";

            String checkDouble = result2.getText();

            for(int i=0;i<checkDouble.length();i++){
                char c = checkDouble.charAt(i); // result2 안에 . 이 있으면 실수가 됨
                if( c== '.'){ // 실수 연산 불가능
                    output += "실수 팩토리얼 연산 불가능";
                    doubleFlag = true; // 실수임
                }
            }
            if(!doubleFlag){ // 실수가 아니면
                int intTemp = Integer.parseInt(result2.getText()); // 정수로 가져와
                int fac = 1;
                for(int i=2 ;i<=intTemp;i++){
                    fac *= i;
                } // 팩토리얼 연산 후
                output += fac; // 결과 저장
            }
            result2.setText(output);
            clearFlag();
        }
        // 증가 단항 연산자
        else if(btnEvent.equals("++")){
            String output = "";
            double doubleTmp = Double.parseDouble(result2.getText());

            if(doubleTmp > 0){
                doubleTmp++;
            }

            if(doubleTmp == (long)doubleTmp) {
                output += (long) doubleTmp;
            }
            else{
                    output += doubleTmp;
            }
            result2.setText(output);
        }
        // 감소 단항 연산자
        else if(btnEvent.equals("--")){
            String output = "";
            double doubleTmp = Double.parseDouble(result2.getText());

            if(doubleTmp > 0){
                doubleTmp--;
            }

            if(doubleTmp == (long)doubleTmp) {
                output += (long) doubleTmp;
            }
            else{
                output += doubleTmp;
            }

            result2.setText(output);
        }
        // 출력하기
        else if (btnEvent.equals("=")) {
            enterOperation();
            try{ // 티모의 웃는 소리가 나니 조심해주세요..
                File file = new File("content/timmo.wav");
                AudioClip audioClip = Applet.newAudioClip(file.toURL());
                audioClip.play();
            }catch(MalformedURLException mue){
                mue.printStackTrace();
            }
        }
        else {
            numOperation(btnEvent);
        }
    }

    public static void main(String[] args) {

        new Calculator();

    }

}
