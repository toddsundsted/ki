package jess.view;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import jess.*;

/**
 A nifty graphical Rete Network viewer for Jess

 @author E.J. Friedman-Hill (C)1997
*/

public class ViewFunctions implements Userpackage
{
  public void add(Rete engine)
  {
    engine.addUserfunction(new View());
  }
}

class NetworkViewer extends Panel implements Observer
{
  Rete m_engine;
  Vector m_rows = new Vector();
  Hashtable m_doneNodes = new Hashtable();
  Label m_lbl;
  long m_lastMD = 0;
  Vector m_frames = new Vector();

  public NetworkViewer(Rete engine)
  {
    m_engine = engine;
    setLayout(new BorderLayout());
    add("South", m_lbl = new Label());

    // Build the network

    for (int i=0; i<m_engine.compiler().roots().size(); i++)
      buildNetwork(((Successor) m_engine.compiler().roots().elementAt(i)).m_node,
                   1);
    message("Network complete");
  }
  
  public void update(Observable obs, Object obj)
  {
    if (obj.equals("RULE"))
      {
        m_doneNodes = new Hashtable();
        m_rows = new Vector();
        for (int i=0; i<m_engine.compiler().roots().size(); i++)
          buildNetwork(((Successor) m_engine.compiler().roots().elementAt(i)).m_node,
                       1);
        message("Network complete");
        repaint();
      }
    for (int i=0; i<m_frames.size(); i++)
      ((NodeView) m_frames.elementAt(i)).describeNode();
  }

  NodeView buildNetwork(Node n, int depth)
  {
    message("Building at depth " + depth);
    NodeView nv;
    if ((nv = (NodeView) m_doneNodes.get(n)) != null)
      // We've done this one already
      return nv;

    Vector row;
    if (m_rows.size() < depth)
      {
        row = new Vector();
        m_rows.addElement(row);
      }
    else
      row = (Vector) m_rows.elementAt(depth-1);
    
    nv = new NodeView(n, depth-1, row.size(), m_frames);
    m_doneNodes.put(n,nv);
    row.addElement(nv);
    for (int i=0; i<n.succ().size(); i++)
      {
        Successor s = (Successor) n.succ().elementAt(i);
        nv.addConnection(buildNetwork(s.m_node, depth + 1), s.m_callType);
      }
    return nv;
  }

  public void paint(Graphics g)
  {
    for (int i=0; i<m_rows.size(); i++)
      {
        Vector row = (Vector) m_rows.elementAt(i);
        for (int j=0; j<row.size(); j++)
          {
            NodeView nv = (NodeView) row.elementAt(j);
            nv.paint(g);
          }
      }
  }
  
  public boolean mouseUp(Event e, int x, int y)
  {
    long t = System.currentTimeMillis();
    if ((t - m_lastMD) < 500)
      {
        int rowidx = y / NodeView.HEIGHT;
        int colidx = (x - (rowidx % 2) * NodeView.SIZE)/ NodeView.WIDTH;
        message ("No node in row " + rowidx + ", col " + colidx);
        if (m_rows.size() < rowidx + 1)
          return false;
        Vector row = (Vector) m_rows.elementAt(rowidx);
        if (row.size() < colidx + 1)
          return false;
        NodeView nv = (NodeView) row.elementAt(colidx);
        nv.fullDisplay();
        message("OPEN!");
        return true;
      }
    m_lastMD = System.currentTimeMillis();
    return false;
  }

  public boolean mouseMove(Event e, int x, int y)
  {
    int rowidx = y / NodeView.HEIGHT;
    int colidx = (x - (rowidx % 2) * NodeView.SIZE)/ NodeView.WIDTH;
    message ("No node in row " + rowidx + ", col " + colidx);
    if (m_rows.size() < rowidx + 1)
      return false;
    Vector row = (Vector) m_rows.elementAt(rowidx);
    if (row.size() < colidx + 1)
      return false;
    NodeView nv = (NodeView) row.elementAt(colidx);
    nv.textDisplay(this);
    return true;
  }



  public void message(String s)
  {
    m_lbl.setText(s);
  }

}

class NodeView implements ActionListener
{
  static final int WIDTH = 20;
  static final int HEIGHT = 20;
  static final int SIZE = 10;
  static final int MARGIN = (WIDTH-SIZE)/2;
  static final int HALF = SIZE/2;
  
  int top, left;
  Node m_node;
  Color c;
  Vector conn = new Vector();
  TextArea m_view;
  Vector m_frames;
  Frame m_f;
  Color[] ctColor = { Color.blue, Color.orange, Color.green, Color.red };

  public NodeView(Node node, int row, int col, Vector v)
  {
    m_node = node;
    m_frames = v;
    top = row*HEIGHT + MARGIN;
    left = col*WIDTH + MARGIN + (row % 2) * SIZE;

    if (m_node instanceof Node1)
      c = Color.red;
    else if (m_node instanceof NodeNot2)
      c = Color.yellow;
    else if (m_node instanceof Node2)
      c = Color.green;
    else if (m_node instanceof NodeTest)
      c = Color.blue;
    else if (m_node instanceof NodeTerm)
      c = Color.cyan;
    else
      c = Color.black;
  }

  public void paint(Graphics g)
  {
    g.setColor(c);
    g.fillRect(left, top, SIZE, SIZE);
    // g.setColor(Color.black);
    // g.drawString("" + conn.size(), left + 2, top + 5);
    for (int i=0; i<conn.size(); i++)
      {
        NVSucc nvs = (NVSucc) conn.elementAt(i);
        g.setColor(ctColor[nvs.callType]);
        g.drawLine(left+HALF, top+SIZE, nvs.nv.left+HALF, nvs.nv.top);
      }
  }

  public void textDisplay(NetworkViewer vwr)
  {
      vwr.message(m_node.toString());
  }

  public void actionPerformed(ActionEvent ae)
  {
    m_frames.removeElement(this);
    if (m_f != null)
      {
        m_f.dispose();
        m_f = null;
      }
  }

  public void fullDisplay()
  {
    if (m_f == null)
      {
        m_f = new Frame("Node View");
        
        m_f.setLayout(new BorderLayout());
        Button b = new Button("Quit");
        b.addActionListener(this);
        
        TextArea ta = new TextArea(40, 20);
        m_view = ta;
        ta.setEditable(false);
        m_f.add("South", b);
        m_f.add("Center", ta);
        describeNode();
        
        m_f.resize(300,300);
        m_f.validate();
        m_f.show();
        m_frames.addElement(this);
      }
  }


  void describeNode()
  {
    StringBuffer sb = new StringBuffer(m_node.toString());
    if (m_node instanceof Node2)
      sb.append(((Node2) m_node).displayMemory());
    else if (m_node instanceof NodeTerm)
      {
        try
          {
             sb.append("\n\n");
             sb.append(((NodeTerm) m_node).rule().ppRule());
             sb.append("\n\n");
             sb.append(((NodeTerm) m_node).rule().listNodes());
          }        
        catch (ReteException re) {}

      }
          
    m_view.setText(sb.toString());
  }

  public void addConnection(NodeView nv, int callType)
  {
    conn.addElement(new NVSucc(nv, callType));
  }
  
}

class NVSucc
{
  int callType;
  NodeView nv;
  NVSucc(NodeView nv, int callType)
  {
    this.nv = nv;
    this.callType = callType;
  }
       
}

class View implements Userfunction
{
  private int m_name = RU.putAtom("view");
  public int name() { return m_name; }
  
  public Value call(ValueVector vv, Context context) throws ReteException
  {
    final Frame f = new Frame("Network View");
    f.setLayout(new BorderLayout());
    NetworkViewer nv = new NetworkViewer(context.engine());
    f.add("Center", nv);
    Button b = new Button("Quit");
    b.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          f.hide();
          f.dispose();
        }
    });
    f.add("South", b);
    f.resize(500,500);
    f.validate();
    f.show();
    
    // NullDisplay implements Observable
    if (context.engine().display() instanceof Observable)
      ((Observable) context.engine().display()).addObserver(nv);

    return Funcall.TRUE();
  }
}
