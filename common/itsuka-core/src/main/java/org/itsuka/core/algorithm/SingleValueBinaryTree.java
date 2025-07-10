package org.itsuka.core.algorithm;

import java.util.*;

class TreeNode {
    int val;

    TreeNode left;

    TreeNode right;
}

public class SingleValueBinaryTree {
    public boolean isSingleValue(TreeNode root) {
        return dfs(root.val, root);
    }

    private boolean dfs(int val, TreeNode node) {
        if (node == null) {
            return true;
        }
        return val == node.val && dfs(val, node.left) && dfs(val, node.right);
    }

    private boolean levelOrder(TreeNode root) {
        Set<Integer> set = new HashSet<>();
        Deque<List<TreeNode>> deque = new LinkedList<>();
        List<TreeNode> nodes = new ArrayList<>();
        nodes.add(root);
        deque.offer(nodes);
        while (!deque.isEmpty()) {
            List<TreeNode> currentTier = deque.pop();
            for (int i = 0; i < currentTier.size(); i++) {
                TreeNode current = currentTier.get(i);
                set.add(current.val);
                if (current.left != null) {
                    currentTier.add(current.left);
                }
                if (current.right != null) {
                    currentTier.add(current.right);
                }
            }
            deque.offer(currentTier);
        }
        return set.size() == 1;
    }
}
