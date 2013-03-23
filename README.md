Ad Hoc Project NetBeans Plugin
==============================

A NetBeans Plugin to make any folder a project.

NetBeans is project-oriented; yet if you do web or similar development, sometimes it can be useful to work with a folder *as if* it were a project, even though it isn't in terms of having a build file or similar.

This plugin lets you make any folder into a project.  It is no-frills, and tries to simply do the Right Thing.  

Download
------------
Get the latest ``nbm`` file from [the continuous build server](https://timboudreau.com/builds/job/Netbeans-Adhoc-Project-Plugin/lastSuccessfulBuild/artifact/adhoc-project/target/).  Install it in the IDE by opening **Tools | Plugins**, on the **Downloaded** tab.


Features
--------

 * **Favorites** - frequently opened files show up under the Favorites node - you tell it what files are most important by using them
 * **Files by Type** - locate files by file type without having to remember what folder they're in
 * **Code Formatting** - projects remember their code formatting settings

![Screen Shot](screenshot.png "NetBeans Ad-Hoc Projects")

The plugin does not write any metadata into project folders - it is all kept in your NetBeans settings directory - so there are no surprises with versioning checkins and such.


License
-------

MIT license - do what thou wilt, give credit where it's due.

