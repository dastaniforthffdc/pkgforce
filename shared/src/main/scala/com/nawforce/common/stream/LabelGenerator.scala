/*
 [The "BSD licence"]
 Copyright (c) 2020 Kevin Jones
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.nawforce.common.stream

import com.nawforce.common.diagnostics.{ERROR_CATEGORY, Issue, Location, PathLocation}
import com.nawforce.common.documents._
import com.nawforce.common.names.Name
import com.nawforce.common.xml.XMLException
import com.nawforce.runtime.xml.XMLDocument

case class LabelFileEvent(path: String) extends PackageEvent
case class LabelEvent(location: PathLocation, name: Name, isProtected: Boolean) extends PackageEvent

object LabelGenerator extends Generator {

  protected def toEvents(document: MetadataDocument): Iterator[PackageEvent] = {
    val source = document.source
    source.value
      .map(source => {
        XMLDocument(document.path, source) match {
          case Left(issue) => Iterator(IssuesEvent(issue))
          case Right(document) =>
            val rootElement = document.rootElement
            try {
              rootElement.assertIs("CustomLabels")
              val labels = rootElement
                .getChildren("labels").iterator
                .flatMap(c => {
                  val fullName: String = c.getSingleChildAsString("fullName")
                  val protect: Boolean = c.getSingleChildAsBoolean("protected")
                  Some(
                    LabelEvent(PathLocation(document.path.toString, Location(c.line)),
                               Name(fullName),
                               protect))
                })
              labels ++ Iterator(LabelFileEvent(document.path.toString))
            } catch {
              case e: XMLException =>
                Iterator(IssuesEvent(Issue(document.path, ERROR_CATEGORY, e.where, e.msg)))
            }
        }
      })
      .getOrElse(Iterator.empty) ++ IssuesEvent.iterator(source.issues)
  }
}