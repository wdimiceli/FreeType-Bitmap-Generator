package com.wdimiceli;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wes DiMiceli on 7/20/2014.
 */
public class SkylineBottomLeft {


    //A class for interal bookkeeping.
    private class Node {
        public int width;
        public int height;
        public Node (int in_width, int in_height) {
            this.width = in_width;
            this.height = in_height;
        }
    }
    private ArrayList<Node> nodes;
    //width and height of the "canvas"
    private int skylineWidth;
    private int skylineHeight;
    public SkylineBottomLeft(int in_skylineWidth, int in_skylineHeight) {
        nodes = new ArrayList<Node>();
        nodes.add(new Node(in_skylineWidth, 0));
        skylineWidth = in_skylineWidth;
        skylineHeight = in_skylineHeight;
    }
    /*
    Returns the highest Y coordinate of all nodes beginning at index that fall within "width" distance.

    This method determines the minimum Y coordinate that would be choosen if a new node of "width"
    were placed at this index.
    */
    private int maxYForNodeIndex(int index, int width) {
        int widthAccum = 0;
        int maxY = 0;
        //iterate through the nodes until we've searched enough
        for (int nodeCount = nodes.size(); index < nodeCount && widthAccum < width; index++) {
            Node node = nodes.get(index);
            maxY = Math.max(maxY, node.height);
            widthAccum += node.width;
        }
        //if we hit the end of skyline, this node is too wide to be placed, so return the max height
        if (widthAccum < width) {
            maxY = skylineHeight;
        }
        return maxY;
    }

    /*
    untested!
     */
    private int maxWidthForNodeIndex(int index) {
        Node targetNode = nodes.get(index);
        int widthAccum = targetNode.width;
        int nodeHeight = targetNode.height;
        for (int nodeCount = nodes.size(); index < nodeCount; index++) {
            Node node = nodes.get(index);
            //break if this node is higher, thus any additional width would collide
            if (node.height > nodeHeight) {
                break;
            }
            widthAccum += node.width;
        }
        return widthAccum;
    }
    /*
    Inserts a new node at this index and merges/resizes nodes that come after it
     */
    private void insertNodeAtIndex(int index, int width, int height) {
        nodes.add(index, new Node(width, height));
        //we search for nodes within this area that will be merged into the new node
        int widthAccum = width;
        index++;
        //stop when we've merged every node that comes after
        while (index < nodes.size()) {
            Node node = nodes.get(index);
            //check the overlap between the new node and the next one
            //if negative we can safely remove it, otherwise resize it and break
            int remainderWidth = node.width - widthAccum;
            if (remainderWidth <= 0) {
                nodes.remove(index);
                widthAccum -= node.width;
            } else {
                node.width = remainderWidth;
                break;
            }
        }
    }
    /*
    Call this consecutively to have the algorithm find the optimal location for
    each rectangle and return the coordinates

     The method returns negative coordinates when it cannot place the requested rectangle
     Usually this means that the skyline is filled up or otherwise that the rectangle is too big
     */
    public Point place(int width, int height) {
        assert width > 0 && height > 0;
        assert width < skylineWidth && height < skylineHeight;
        Point retval = null;
        int minY = skylineHeight;
        int minX = 0;
        int minNodeIndex = -1;
        int xAccum = 0;
        int nodeCount = nodes.size();
        //iterate across each of our nodes and find the index with the smallest Y coordinate
        for (int i = 0; i < nodeCount; i++) {
            //this is the minimum Y that we can use if we place the new node here
            int placedY = this.maxYForNodeIndex(i, width);
            if (placedY < minY) {
                minY = placedY;
                minX = xAccum;
                //remember this index so we can put a new node here later
                minNodeIndex = i;
            }
            //accumulate the width so we can return the X coordinate for our users
            xAccum += nodes.get(i).width;
        }
        int nodeHeight = minY + height;
        //make sure the lowest Y is low enough to place, otherwise this rectangle is too tall
        if (nodeHeight < skylineHeight) {
            retval = new Point(minX, minY);
            this.insertNodeAtIndex(minNodeIndex, width, nodeHeight);
        }
        return retval;
    }

    /*
    returns the bounding box of the currently filled area
    always starts at (0, 0) with the returned width and height
     */
    public Point boundingBox() {
        Point retval = new Point(0,0);
        int accumWidth = 0;
        for (Node n : nodes) {
            if (n.height > 0) {
                retval.x = accumWidth + n.width;
                retval.y = Math.max(retval.y, n.height);
            }
            accumWidth += n.width;
        }
        return retval;
    }

    /*
    computes the remaining empty as a series of vertical stripes above the skyline
     */
    public int emptyArea() {
        int area = 0;
        for (Node n : nodes) {
            area += n.width * (skylineHeight - n.height);
        }
        return area;
    }

    public int filledArea() {
        int area = 0;
        for (Node n : nodes) {
            area += n.width * n.height;
        }
        return area;
    }

    /*
    returns a list information about each node
        the height value in the rectangle corresponds to the area ABOVE the skyline
     */
    public List<Rectangle> getSkyline() {
        ArrayList<Rectangle> retval = new ArrayList<Rectangle>();
        int accumWidth = 0;
        for (Node n : nodes) {
            retval.add(new Rectangle(accumWidth, n.height, n.width, skylineHeight - n.height));
            accumWidth += n.width;
        }
        return retval;
    }

    /*
    Expands the canvas to a new size.  Must be larger then the current size.
     */
    public void expand(int newWidth, int newHeight) {
        assert newWidth > skylineWidth && newHeight > skylineHeight;
        nodes.add(new Node(newWidth - skylineWidth, 0));
        skylineWidth = newWidth;
        skylineHeight = newHeight;
    }

    public void shrink(int newWidth, int newHeight) {
        assert newWidth > 0 && newHeight > 0;
        //shrink by this much
        int diffWidth = newWidth - skylineWidth;
        //remove nodes until we have no more, or until we've removed exactly the right amount
        while (nodes.size() > 0 && diffWidth > 0) {
            int index = nodes.size()-1;
            Node n = nodes.get(index);
            //removing this node is too much, so resize it and exit
            if (diffWidth - n.width < 0) {
                n.width -= diffWidth;
                break;
            } else {
                nodes.remove(index);
                diffWidth -= n.width;
            }
        }
        skylineWidth = newWidth;
        skylineHeight = newHeight;
    }
}
