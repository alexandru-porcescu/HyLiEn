package eu.unicredit.web

import java.net.URL

import com.machinepublishers.jbrowserdriver.{ JBrowserDriver, Settings, Timezone }
import eu.unicredit.web.Models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.openqa.selenium.{ By, WebElement }

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Created by fabiofumarola on 24/05/16.
 */
class VisualWebExtractor(headless: Boolean = true, quickRender: Boolean = true) {

  private val settings = Settings.builder()
    .timezone(Timezone.EUROPE_ROME)
    .headless(headless)
    .cache(true)
    .quickRender(quickRender)
    .ajaxWait(300)
    .build()

  private val driver = new JBrowserDriver(settings)

  /**
   *
   * @param url
   * @return the root of the parsed tree
   */
  def parse(url: String): VisualDomNode = {
    driver.get(url)
    val body = driver.findElementByTagName("body")
    val root = toDomNode(1, body)
    val queue = mutable.Queue(children(body).map((root, _)): _*)

    var counter = 2
    while (queue.nonEmpty) {
      val (parent, e) = queue.dequeue()
      val node = toDomNode(counter, e)
      counter += 1
      parent.children.append(node)
      queue.enqueue(children(e).map((node, _)): _*)
    }
    root
  }

  private def children(e: WebElement) =
    e.findElements(By.xpath("child::*")).filter(_.isDisplayed)

  private def toDomNode(id: Int, e: WebElement) = VisualDomNode(
    id = id,
    tagName = e.getTagName,
    cssClasses = e.getAttribute("class"),
    location = Location(e.getLocation.x, e.getLocation.y),
    size = Size(e.getSize.width, e.getSize.height),
    text = e.getText)

  def close() = driver.close()

}

class JSoupExtractor {

  def parse(url: String): DomNode = {
    val doc = Jsoup.parse(new URL(url), 2000)
    val body = doc.body()
    val root = toDomNode(1, body)
    val queue = mutable.Queue(body.children()
      .filterNot(_.cssSelector().contains("script")).map((root, _)): _*)

    var counter = 2
    while (queue.nonEmpty) {
      val (parent, e) = queue.dequeue()
      val node = toDomNode(counter, e)
      counter += 1
      parent.children.append(node)
      queue.enqueue(e.children().map((root, _)): _*)
    }

    root
  }

  private def toDomNode(id: Int, e: Element) = DomNode(
    id = id,
    tagName = e.tagName(),
    cssClasses = e.className(),
    cssSelector = e.cssSelector(),
    text = e.ownText())
}