package io.github.kotlinreladomo.generator.parser

import io.github.kotlinreladomo.generator.model.ParsedMithraObject
import io.github.kotlinreladomo.generator.types.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Enhanced XML parser with type safety and comprehensive validation
 */
class EnhancedXmlParser {
    
    fun parseXmlFile(xmlFile: File): ParsedMithraObject {
        require(xmlFile.exists()) { "XML file does not exist: ${xmlFile.absolutePath}" }
        require(xmlFile.extension == "xml") { "File must have .xml extension: ${xmlFile.name}" }
        
        val document = parseDocument(xmlFile)
        val rootElement = document.documentElement
        
        return when (rootElement.tagName) {
            "MithraObject" -> parseMithraObject(rootElement)
            else -> throw IllegalArgumentException("Unknown root element: ${rootElement.tagName}")
        }
    }
    
    private fun parseDocument(xmlFile: File): Document {
        return try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            dBuilder.parse(xmlFile).apply { 
                documentElement.normalize() 
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse XML file: ${xmlFile.name}", e)
        }
    }
    
    private fun parseMithraObject(element: Element): ParsedMithraObject {
        val className = element.getAttribute("objectClass")
            ?.substringAfterLast('.')
            ?: throw IllegalArgumentException("objectClass attribute is required")
            
        val packageName = element.getAttribute("objectClass")
            ?.substringBeforeLast('.')
            ?: throw IllegalArgumentException("objectClass must include package name")
            
        val tableName = element.getAttribute("table")
            ?: className.toUpperCase()
            
        val superClass = element.getAttribute("superClass")?.takeIf { it.isNotBlank() }
        val defaultTable = element.getAttribute("defaultTable")?.takeIf { it.isNotBlank() }
        
        // Parse all attributes
        val attributes = mutableListOf<AttributeType>()
        attributes.addAll(parseSimpleAttributes(element))
        attributes.addAll(parseAsOfAttributes(element))
        attributes.addAll(parseRelationships(element))
        
        // Determine object type
        val isReadOnly = element.getAttribute("readOnly") == "true"
        val asOfAttributes = attributes.filterIsInstance<AttributeType.AsOfAttribute>()
        val objectType = ObjectType.fromAsOfAttributes(asOfAttributes, isReadOnly)
        
        return ParsedMithraObject(
            className = className,
            packageName = packageName,
            tableName = tableName,
            attributes = attributes,
            objectType = objectType,
            defaultTableName = defaultTable,
            superClass = superClass
        )
    }
    
    private fun parseSimpleAttributes(parent: Element): List<AttributeType.Simple> {
        val attributes = mutableListOf<AttributeType.Simple>()
        
        val attributeNodes = parent.getElementsByTagName("Attribute")
        for (i in 0 until attributeNodes.length) {
            val node = attributeNodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                attributes.add(parseAttribute(node as Element))
            }
        }
        
        return attributes
    }
    
    private fun parseAttribute(element: Element): AttributeType.Simple {
        val name = element.getAttribute("name")
            ?: throw IllegalArgumentException("Attribute name is required")
            
        val javaType = element.getAttribute("javaType")
            ?: throw IllegalArgumentException("Attribute javaType is required for $name")
            
        val nullable = element.getAttribute("nullable") != "false"
        val columnName = element.getAttribute("columnName")?.takeIf { it.isNotBlank() }
        val primaryKey = element.getAttribute("primaryKey") == "true"
        val identity = element.getAttribute("identity") == "true"
        val trim = element.getAttribute("trim") == "true"
        val pooled = element.getAttribute("pooled") == "true"
        
        val type = TypeMapper.fromXmlType(javaType)
        
        return AttributeType.Simple(
            name = name,
            type = type,
            nullable = nullable,
            columnName = columnName,
            primaryKey = primaryKey,
            identity = identity,
            trim = trim,
            pooled = pooled
        )
    }
    
    private fun parseAsOfAttributes(parent: Element): List<AttributeType.AsOfAttribute> {
        val attributes = mutableListOf<AttributeType.AsOfAttribute>()
        
        val asOfNodes = parent.getElementsByTagName("AsOfAttribute")
        for (i in 0 until asOfNodes.length) {
            val node = asOfNodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                attributes.add(parseAsOfAttribute(node as Element))
            }
        }
        
        return attributes
    }
    
    private fun parseAsOfAttribute(element: Element): AttributeType.AsOfAttribute {
        val name = element.getAttribute("name")
            ?: throw IllegalArgumentException("AsOfAttribute name is required")
            
        val fromColumn = element.getAttribute("fromColumnName")
            ?: throw IllegalArgumentException("fromColumnName is required for AsOfAttribute $name")
            
        val toColumn = element.getAttribute("toColumnName")
            ?: throw IllegalArgumentException("toColumnName is required for AsOfAttribute $name")
            
        val infinityDate = element.getAttribute("infinityDate")?.takeIf { it.isNotBlank() }
        val isProcessingDate = element.getAttribute("isProcessingDate") == "true"
        val timezoneConversion = TimezoneConversion.fromString(element.getAttribute("timezoneConversion"))
        
        // Determine temporal type
        val temporalType = when {
            isProcessingDate -> TemporalType.PROCESSING_DATE
            name.lowercase().contains("processing") -> TemporalType.PROCESSING_DATE
            else -> TemporalType.BUSINESS_DATE
        }
        
        return AttributeType.AsOfAttribute(
            name = name,
            type = temporalType,
            fromColumnName = fromColumn,
            toColumnName = toColumn,
            infinityDate = infinityDate,
            isProcessingDate = isProcessingDate,
            timezoneConversion = timezoneConversion
        )
    }
    
    private fun parseRelationships(parent: Element): List<AttributeType.Relationship> {
        val relationships = mutableListOf<AttributeType.Relationship>()
        
        val relationshipNodes = parent.getElementsByTagName("Relationship")
        for (i in 0 until relationshipNodes.length) {
            val node = relationshipNodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                relationships.add(parseRelationship(node as Element))
            }
        }
        
        return relationships
    }
    
    private fun parseRelationship(element: Element): AttributeType.Relationship {
        val name = element.getAttribute("name")
            ?: throw IllegalArgumentException("Relationship name is required")
            
        val relatedObject = element.getAttribute("relatedObject")
            ?: throw IllegalArgumentException("relatedObject is required for relationship $name")
            
        val cardinalityStr = element.getAttribute("cardinality")
            ?: throw IllegalArgumentException("cardinality is required for relationship $name")
            
        val cardinality = parseCardinality(cardinalityStr)
        val reverseRelationshipName = element.getAttribute("reverseRelationshipName")?.takeIf { it.isNotBlank() }
        val orderBy = element.getAttribute("orderBy")?.takeIf { it.isNotBlank() }
        
        // Parse relationship parameters
        val parameters = mutableListOf<RelationshipParameter>()
        val paramElements = element.getElementsByTagName("RelationshipParameter")
        for (i in 0 until paramElements.length) {
            val paramNode = paramElements.item(i)
            if (paramNode.nodeType == Node.ELEMENT_NODE) {
                val paramElement = paramNode as Element
                parameters.add(
                    RelationshipParameter(
                        from = paramElement.getAttribute("from") 
                            ?: throw IllegalArgumentException("from is required for RelationshipParameter"),
                        to = paramElement.getAttribute("to")
                            ?: throw IllegalArgumentException("to is required for RelationshipParameter")
                    )
                )
            }
        }
        
        return AttributeType.Relationship(
            name = name,
            relatedObject = relatedObject,
            cardinality = cardinality,
            reverseRelationshipName = reverseRelationshipName,
            parameters = parameters,
            orderBy = orderBy
        )
    }
    
    private fun parseCardinality(value: String): Cardinality = when (value.lowercase()) {
        "one-to-one" -> Cardinality.ONE_TO_ONE
        "one-to-many" -> Cardinality.ONE_TO_MANY
        "many-to-one" -> Cardinality.MANY_TO_ONE
        "many-to-many" -> Cardinality.MANY_TO_MANY
        else -> throw IllegalArgumentException("Unknown cardinality: $value")
    }
}