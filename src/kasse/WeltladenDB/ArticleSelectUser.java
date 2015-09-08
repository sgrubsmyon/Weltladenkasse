package WeltladenDB;

/**
 * Interface that should be implemented by all classes that want to use an
 * instance of the ArticleSelectPanelGrundlage.
 *
 * Its purpose is that there must be a method to update the selectedArticleID
 * once it changes in the ArticleSelectPanel.
 */
public interface ArticleSelectUser {
    public void updateSelectedArticleID(int selectedArticleID);
}
