const axios = require('axios');
// https://www.npmjs.com/package/google-auth-library
const { GoogleAuth } = require('google-auth-library');
const fs = require('fs');
const location = process.env.LOCATION;
const project = process.env.PROJECT;
const job = process.env.SEARCH_JOB;

async function getAccessToken() {
  const secretName = process.env.SECRET_NAME;
  const secretB64 = fs.readFileSync(`/secrets/${secretName}`)
  const secretStr2 = Buffer.from(secretB64, 'base64').toString();
  const secretStr = Buffer.from(secretStr2, 'base64').toString();
  const secret = JSON.parse(secretStr);

  // use the key to authenticate the axios request
  const auth = new GoogleAuth({
	credentials: {
	  client_email: secret.client_email,
	  private_key: secret.private_key
	},
	scopes: ['https://www.googleapis.com/auth/cloud-platform']
  });
  const accessToken = await auth.getAccessToken();
  return accessToken;
}

async function runSearchJob(accessToken) {
  const runUrl = `https://${location}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${project}/jobs/${job}:run`;
  console.log(`Calling ${runUrl}`);
  const config = {
	headers: {
	  Authorization: `Bearer ${accessToken}`
	}
  };
  const data = {}
  return axios.post(runUrl, data, config);
}

/**
 * Triggered by a change to a Firestore document.
 *
 * @param {!Object} event Event payload.
 * @param {!Object} context Metadata for the event.
 */
exports.searchRequestWriteTrigger = (event, context) => {
  const resource = context.resource;
  // log out the resource string that triggered the function
  console.log('Function triggered by change to: ' +  resource);
  // now log the full event object
  console.log(JSON.stringify(event));
  console.log(JSON.stringify(context));
  const url = `https://${location}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${project}/jobs/${job}:run`;

  const accessToken = getAccessToken();
  const res = runSearchJob(accessToken);
  return res;
};
