package controllers

import io.rampant.vulgar.security.AuthTokenManager
import io.rampant.vulgar.utils.UserAgent
import models.UserToken
import models.db.{UserEmails, UserTokens, Users}
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{Action, Controller, Cookie}

import scala.concurrent.Future

object Debug extends Controller {

	def login = Action.async {
		request =>
			UserEmails.findByEmail("jonathan@borzilleri.net").flatMap({
				case None => Future.successful(Unauthorized);
				case Some(e) =>
					val t = UserToken.makeToken
					val b = UserAgent.makeString(request.headers.get("User-Agent").getOrElse(""))
					UserTokens.insert(UserToken(None, e.userId, t, java.time.Instant.now(), sessionOnly = false, b, None))
						.map({ token => Redirect(routes.Application.index())
						.withCookies(Cookie(AuthTokenManager.tokenCookieName, t, None, httpOnly = true))
					})
			})
	}

	def debug = Action.async {
		Users.list.flatMap({ users =>
			Future.sequence(users.map({ u =>
				for {
					t <- UserTokens.list(u)
					e <- UserEmails.list(u)
				} yield {
					Json.toJson(u).as[JsObject] +
						("tokens" -> Json.toJson(t)) +
						("emails" -> Json.toJson(e))
				}
			})).map(JsArray).map(Ok(_))
		})
	}

	def useragent = Action {
		request =>
			Ok(UserAgent.parseFromString(request.headers.get("User-Agent").get).toString)
	}
}
