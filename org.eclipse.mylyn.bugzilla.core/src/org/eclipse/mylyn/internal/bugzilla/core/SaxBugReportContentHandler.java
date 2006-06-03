/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.bugzilla.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.mylar.internal.tasklist.RepositoryTaskData;
import org.eclipse.mylar.internal.tasklist.Comment;
import org.eclipse.mylar.internal.tasklist.RepositoryAttachment;
import org.eclipse.mylar.internal.tasklist.RepositoryTaskAttribute;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for xml bugzilla reports.
 * 
 * @author Rob Elves
 */
public class SaxBugReportContentHandler extends DefaultHandler {

	private static final String COMMENT_ATTACHMENT_STRING = "Created an attachment (id=";

	private StringBuffer characters;

	private Comment comment;

	private final Map<Integer, Comment> attachIdToComment = new HashMap<Integer, Comment>();
	
	private int commentNum = 0;

	private RepositoryAttachment attachment;

	private RepositoryTaskData report;

	private String errorMessage = null;

	public SaxBugReportContentHandler(RepositoryTaskData rpt) {
		this.report = rpt;
	}

	public boolean errorOccurred() {
		return errorMessage != null;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public RepositoryTaskData getReport() {
		return report;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		characters.append(ch, start, length);
//		 if (monitor.isCanceled()) {
//		 throw new OperationCanceledException("Search cancelled");
//		 }
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		characters = new StringBuffer();
		BugzillaReportElement tag = BugzillaReportElement.UNKNOWN;
		try {
			tag = BugzillaReportElement.valueOf(localName.trim().toUpperCase());
		} catch (RuntimeException e) {
			if (e instanceof IllegalArgumentException) {
				// ignore unrecognized tags
				return;
			}
			throw e;
		}
		switch (tag) {
		case BUGZILLA:
			// Note: here we can get the bugzilla version if necessary
			break;
		case BUG:
			if (attributes != null && (attributes.getValue("error") != null)) {
				errorMessage = attributes.getValue("error");
			}
			break;
		case LONG_DESC:
			comment = new Comment(report, commentNum++);
			break;
		case ATTACHMENT:
			attachment = new RepositoryAttachment();
			if (attributes != null && (attributes.getValue(BugzillaReportElement.IS_OBSOLETE.getKeyString()) != null)) {
				attachment.addAttribute(BugzillaReportElement.IS_OBSOLETE.getKeyString(), BugzillaRepositoryUtil.makeNewAttribute(
						BugzillaReportElement.IS_OBSOLETE));
			}
			break;
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		BugzillaReportElement tag = BugzillaReportElement.UNKNOWN;
		try {
			tag = BugzillaReportElement.valueOf(localName.trim().toUpperCase());
		} catch (RuntimeException e) {
			if (e instanceof IllegalArgumentException) {
				// ignore unrecognized tags
				return;
			}
			throw e;
		}
		switch (tag) {
		case BUG_ID: {
			try {
				if (report.getId() != Integer.parseInt(characters.toString())) {
					errorMessage = "Requested report number does not match returned report number.";
				}
			} catch (NumberFormatException e) {
				errorMessage = "Bug id from server did not match requested id.";
			}

			RepositoryTaskAttribute attr = report.getAttribute(tag.getKeyString());
			if (attr == null) {
				attr = BugzillaRepositoryUtil.makeNewAttribute(tag);
				report.addAttribute(tag.getKeyString(), attr);
			}
			attr.setValue(characters.toString());
			break;
		}

			// Comment attributes
		case WHO:
		case BUG_WHEN:
			if (comment != null) {
				RepositoryTaskAttribute attr = BugzillaRepositoryUtil.makeNewAttribute(tag);
				attr.setValue(characters.toString());
				// System.err.println(">>> "+comment.getNumber()+"
				// "+characters.toString());
				comment.addAttribute(tag.getKeyString(), attr);
			}
			break;
		case THETEXT:
			if (comment != null) {
				RepositoryTaskAttribute attr = BugzillaRepositoryUtil.makeNewAttribute(tag);
				attr.setValue(characters.toString());
				// System.err.println(">>> "+comment.getNumber()+"
				// "+characters.toString());
				comment.addAttribute(tag.getKeyString(), attr);

				// Check for attachment
				parseAttachment(comment, characters.toString());
			}
			break;
		case LONG_DESC:
			if (comment != null) {
				report.addComment(comment);
			}
			break;

		// Attachment attributes
		case ATTACHID:
		case DATE:
		case DESC:
		case FILENAME:
		case CTYPE:
		case TYPE:
			if (attachment != null) {
				RepositoryTaskAttribute attr = BugzillaRepositoryUtil.makeNewAttribute(tag);
				attr.setValue(characters.toString());
				attachment.addAttribute(tag.getKeyString(), attr);
			}
			break;
		case DATA:
			// TODO: Need to figure out under what circumstanceswhen attachments
			// are inline and
			// what to do with them.
			break;
		case ATTACHMENT:
			if (attachment != null) {
				report.addAttachment(attachment);
			}
			break;

		// IGNORED ELEMENTS
		case REPORTER_ACCESSIBLE:
		case CLASSIFICATION_ID:
		case CLASSIFICATION:
		case CCLIST_ACCESSIBLE:
		case EVERCONFIRMED:
		case BUGZILLA:
			break;
		case BUG:
			// Reached end of bug. Need to set LONGDESCLENGTH to number of
			// comments
			RepositoryTaskAttribute numCommentsAttribute = report
					.getAttribute(BugzillaReportElement.LONGDESCLENGTH.getKeyString());
			if (numCommentsAttribute == null) {
				numCommentsAttribute = BugzillaRepositoryUtil.makeNewAttribute(BugzillaReportElement.LONGDESCLENGTH);
				numCommentsAttribute.setValue("" + report.getComments().size());
				report.addAttribute(BugzillaReportElement.LONGDESCLENGTH.getKeyString(), numCommentsAttribute);
			} else {
				numCommentsAttribute.setValue("" + report.getComments().size());
			}
			
			// Set the creator name on all attachments
			for (RepositoryAttachment attachment: report.getAttachments()) {
				Comment comment = (Comment)attachIdToComment.get(attachment.getId());
				if(comment != null) {
					attachment.setCreator(comment.getAuthor());
				}
			}
			break;

		// All others added as report attribute
		default:
			RepositoryTaskAttribute attribute = report.getAttribute(tag.getKeyString());
			if (attribute == null) {
				// System.err.println(">>> Undeclared attribute added: " +
				// tag.toString()+" value: "+characters.toString());
				attribute = BugzillaRepositoryUtil.makeNewAttribute(tag);
				attribute.setValue(characters.toString());
				report.addAttribute(tag.getKeyString(), attribute);
			} else {
				// System.err.println("Attr: " + attribute.getName() + " = " +
				// characters.toString());
				attribute.addValue(characters.toString());
			}
			break;
		}

	}

	/** determines attachment id from comment */
	private void parseAttachment(Comment comment, String commentText) {

		int attachmentID = -1;

		if (commentText.startsWith(COMMENT_ATTACHMENT_STRING)) {
			try {
				int endIndex = commentText.indexOf(")");
				if (endIndex > 0 && endIndex < commentText.length()) {
					attachmentID = Integer
							.parseInt(commentText.substring(COMMENT_ATTACHMENT_STRING.length(), endIndex));					
					comment.setHasAttachment(true);
					comment.setAttachmentId(attachmentID);
					attachIdToComment.put(attachmentID, comment);
				}
			} catch (NumberFormatException e) {
				return;
			}
		}
	}

}
