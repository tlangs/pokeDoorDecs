
/**
 * Created by trevyn on 1/7/15.
 */

@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')
import groovy.json.*
import groovyx.net.http.HTTPBuilder

import java.awt.Color
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.*
import java.awt.Font

class Person {

    String name
    BufferedImage sprite
    BufferedImage image
    String pokemon

    Person(String name) {
        this.name = name
    }
}
def input
try {
    if (args.size() == 0) {
        throw new UnsupportedOperationException("Please provide a file.")
    }
    input = new File(args[0])
    if (!input.exists()) {
        throw new FileNotFoundException("The file was not found.")
    }
} catch(Exception e) {
    println e.getMessage()
    System.exit(0)
}

def slurper = new JsonSlurper()

def http = new HTTPBuilder("http://pokeapi.co/")
def names = slurper.parse(new File("residentassistants.json"))
def people = []

names.each { obj ->
    def person = new Person(obj.name)
    person.pokemon = obj.pokemon
    people.add(person)
}

println "Parsed ${people.size()} names."


people.each { person ->
    try {
        def pokedex = slurper.parseText(new URL("http://pokeapi.co/api/v1/pokedex/1").text)
        def mon = pokedex["pokemon"].find {a -> a.name == person.pokemon}
        def info2 = slurper.parseText(new URL("http://pokeapi.co/${mon.resource_uri}").text)
        def spriteInfo2 = slurper.parseText(new URL("http://pokeapi.co/${info2.sprites[0].resource_uri}").text)
        person.sprite = ImageIO.read(new URL("http://pokeapi.co/${spriteInfo2.image}"))
        println "Found image for ${person.name}."
    } catch (IOException e) {
        println "Could not get image from URL"
        println e.getMessage();
    }
}

people.each { person ->
    def image = new BufferedImage((person.sprite.getWidth() * 3) + 50 ,(person.sprite.getHeight() * 3) + 70, BufferedImage.TYPE_INT_ARGB)
    def g = (Graphics2D)image.getGraphics()
    float size = 120F
    def font = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/PokemonSolid.ttf"))
    g.setFont(font.deriveFont(size))
    FontMetrics fontMetrics = g.getFontMetrics()
    while (fontMetrics.stringWidth(person.name) > person.sprite.getWidth() * 3 - 50) {
        size--
        g.setFont(font.deriveFont(size))
        fontMetrics = g.getFontMetrics()
    }

    g.setColor(Color.WHITE)
    g.fillRect(0, 0, image.getWidth(), image.getHeight())

    g.drawImage(person.sprite, 25, 0, person.sprite.getWidth() * 3 , person.sprite.getHeight() * 3, null)

    g.setColor(Color.BLACK)
    g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_GASP)

    def fontht = fontMetrics.getHeight()
    g.drawString(person.name, 50, (person.sprite.getHeight() * 3) + fontht/4)

    person.image = image
}

println "Retrieved pokemon sprites."

def dir = new File("decs")
if (!dir.exists()) {
    dir.mkdir()
}
dir.eachFile { file ->
    file.delete()
}

people.each { person  ->
    ImageIO.write(person.image, "png", new File("decs/${person.name}.png"))
}

println "Done! Processed ${people.size()} people."
