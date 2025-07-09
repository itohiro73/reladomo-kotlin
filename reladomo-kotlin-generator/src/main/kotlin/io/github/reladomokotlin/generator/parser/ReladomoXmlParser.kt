package io.github.reladomokotlin.generator.parser

import io.github.reladomokotlin.generator.model.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parser for Reladomo XML definition files.
 */
class ReladomoXmlParser {
    
    private val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    
    /**
     * Parse a Reladomo XML file into a MithraObjectDefinition.
     */
    fun parse(xmlFile: File): MithraObjectDefinition {
        require(xmlFile.exists()) { "XML file does not exist: ${xmlFile.absolutePath}" }
        require(xmlFile.extension == "xml") { "File must be an XML file: ${xmlFile.name}" }
        
        val document = documentBuilderFactory.newDocumentBuilder().parse(xmlFile)
        document.documentElement.normalize()
        
        return parseMithraObject(document.documentElement)
    }
    
    /**
     * Parse XML content string into a MithraObjectDefinition.
     */
    fun parseXml(xmlContent: String): MithraObjectDefinition {
        val document = documentBuilderFactory.newDocumentBuilder()
            .parse(xmlContent.byteInputStream())
        document.documentElement.normalize()
        
        return parseMithraObject(document.documentElement)
    }
    
    private fun parseMithraObject(root: Element): MithraObjectDefinition {
        val objectType = parseObjectType(root.getAttribute("objectType"))
        val packageName = root.getElementsByTagName("PackageName").item(0)?.textContent
            ?: throw IllegalArgumentException("PackageName is required")
        val className = root.getElementsByTagName("ClassName").item(0)?.textContent
            ?: throw IllegalArgumentException("ClassName is required")
        val tableName = root.getElementsByTagName("DefaultTable").item(0)?.textContent
            ?: className.toUpperCase()
        
        return MithraObjectDefinition(
            packageName = packageName,
            className = className,
            tableName = tableName,
            objectType = objectType,
            attributes = parseAttributes(root),
            asOfAttributes = parseAsOfAttributes(root),
            relationships = parseRelationships(root)
        )
    }
    
    private fun parseObjectType(value: String?): ObjectType {
        return when (value?.lowercase()) {
            "transactional" -> ObjectType.TRANSACTIONAL
            "read-only", "readonly" -> ObjectType.READ_ONLY
            "dated-transactional" -> ObjectType.DATED_TRANSACTIONAL
            "dated-read-only" -> ObjectType.DATED_READ_ONLY
            else -> ObjectType.TRANSACTIONAL
        }
    }
    
    private fun parseAttributes(root: Element): List<AttributeDefinition> {
        val nodeList = root.getElementsByTagName("Attribute")
        return (0 until nodeList.length).mapNotNull { i ->
            val node = nodeList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                parseAttribute(node as Element)
            } else null
        }
    }
    
    private fun parseAttribute(element: Element): AttributeDefinition {
        return AttributeDefinition(
            name = element.getAttribute("name"),
            javaType = element.getAttribute("javaType"),
            columnName = element.getAttribute("columnName"),
            isPrimaryKey = element.getAttribute("primaryKey") == "true",
            nullable = element.getAttribute("nullable") != "false",
            maxLength = element.getAttribute("maxLength").toIntOrNull()
        )
    }
    
    private fun parseAsOfAttributes(root: Element): List<AsOfAttributeDefinition> {
        val nodeList = root.getElementsByTagName("AsOfAttribute")
        return (0 until nodeList.length).mapNotNull { i ->
            val node = nodeList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                parseAsOfAttribute(node as Element)
            } else null
        }
    }
    
    private fun parseAsOfAttribute(element: Element): AsOfAttributeDefinition {
        return AsOfAttributeDefinition(
            name = element.getAttribute("name"),
            fromColumn = element.getAttribute("fromColumnName"),
            toColumn = element.getAttribute("toColumnName"),
            toIsInclusive = element.getAttribute("toIsInclusive") != "false",
            infinityDate = element.getAttribute("infinityDate").ifBlank { null },
            defaultIfNotSpecified = element.getAttribute("defaultIfNotSpecified").ifBlank { null }
        )
    }
    
    private fun parseRelationships(root: Element): List<RelationshipDefinition> {
        val relationships = mutableListOf<RelationshipDefinition>()
        
        // Parse different relationship types
        relationships.addAll(parseRelationshipType(root, "Relationship", Cardinality.MANY_TO_ONE))
        relationships.addAll(parseRelationshipType(root, "RelationshipOneToOne", Cardinality.ONE_TO_ONE))
        relationships.addAll(parseRelationshipType(root, "RelationshipOneToMany", Cardinality.ONE_TO_MANY))
        relationships.addAll(parseRelationshipType(root, "RelationshipManyToMany", Cardinality.MANY_TO_MANY))
        
        return relationships
    }
    
    private fun parseRelationshipType(
        root: Element,
        tagName: String,
        cardinality: Cardinality
    ): List<RelationshipDefinition> {
        val nodeList = root.getElementsByTagName(tagName)
        return (0 until nodeList.length).mapNotNull { i ->
            val node = nodeList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                parseRelationship(node as Element, cardinality)
            } else null
        }
    }
    
    private fun parseRelationship(element: Element, cardinality: Cardinality): RelationshipDefinition {
        val parameters = mutableMapOf<String, String>()
        
        // Extract relationship parameters from child nodes
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val child = childNodes.item(i)
            if (child.nodeType == Node.ELEMENT_NODE && child.textContent.isNotBlank()) {
                parameters[child.nodeName] = child.textContent
            }
        }
        
        return RelationshipDefinition(
            name = element.getAttribute("name"),
            relatedObject = element.getAttribute("relatedObject"),
            cardinality = cardinality,
            reverseRelationshipName = element.getAttribute("reverseRelationshipName").ifBlank { null },
            parameters = parameters
        )
    }
}