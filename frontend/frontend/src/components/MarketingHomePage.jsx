import React from 'react';
import { Link } from 'react-router-dom';
import { getCurrentUser } from '../services/auth';
import { getDefaultRouteForUser } from '../services/permissions';
import './MarketingHomePage.css';

const manualTriageProblems = [
  {
    metric: '1',
    label: 'person bottleneck',
    detail: 'Manual triage usually depends on one coordinator who becomes a queue manager instead of doing their own work.',
  },
  {
    metric: '500+',
    label: 'incident spikes',
    detail: 'During major outages, hundreds of ServiceNow tickets can arrive faster than a person can classify and assign them.',
  },
  {
    metric: 'SLA',
    label: 'risk window',
    detail: 'A missed owner, leave conflict, or overloaded responder can quietly turn into delayed resolution and business impact.',
  },
];

const platformHighlights = [
  {
    eyebrow: 'Admin Command Center',
    title: 'Design the operating model once.',
    description:
      'Create organizations, teams, geos, shifts, schedules, CI ownership, notification rules, and ServiceNow connections from the web app.',
  },
  {
    eyebrow: 'Assignment Intelligence',
    title: 'Let routing follow real availability.',
    description:
      'InciTeam checks ownership, coverage, leave, breaks, active shift, and critical workload before choosing who should receive an incident.',
  },
  {
    eyebrow: 'Operational Evidence',
    title: 'Explain every decision after the moment passes.',
    description:
      'Poll logs, skipped reasons, assignment outcomes, diagnostics, and summary health make incident operations auditable for recent history.',
  },
];

const useCases = [
  {
    role: 'Incident Managers',
    title: 'Move fast when the queue gets noisy.',
    description:
      'See whether the team is covered, validate setup, trigger authorized manual polls, and inspect outcomes without chasing spreadsheets.',
  },
  {
    role: 'Team Admins',
    title: 'Keep ServiceNow routing aligned with the team.',
    description:
      'Manage CI owners, schedules, leaves, breaks, and team access so assignment rules reflect the real support model.',
  },
  {
    role: 'Responders',
    title: 'Know what changed and why you were selected.',
    description:
      'Use mobile views for roster status, logs, assignment results, summaries, and account controls after setup is complete.',
  },
];

const workflowSteps = [
  {
    step: '01',
    title: 'Create your organization and teams',
    description: 'Set up the workspace, teams, geos, shifts, members, roles, and ServiceNow connection once in the web app.',
  },
  {
    step: '02',
    title: 'Map CIs to the right users',
    description: 'Use CI-user mapping to define who owns each ServiceNow configuration item and in what order they should be considered.',
  },
  {
    step: '03',
    title: 'View roster coverage',
    description: 'See who is available across teams, geos, and shifts before an incident needs attention.',
  },
  {
    step: '04',
    title: 'Respect leave, breaks, and critical work',
    description: 'Avoid assigning incidents to people who are away, on break, or already handling critical P0/P1 work.',
  },
  {
    step: '05',
    title: 'Poll ServiceNow automatically',
    description: 'Check ServiceNow every 5 minutes, with manual polling available for authorized users when teams need it now.',
  },
  {
    step: '06',
    title: 'Assign with round robin fairness',
    description: 'Route incidents across eligible mapped owners instead of overloading the same person again and again.',
  },
  {
    step: '07',
    title: 'Notify through Slack, email, and mobile',
    description: 'Send the assignment signal through the channels teams already watch, including mobile push notifications.',
  },
  {
    step: '08',
    title: 'Run without manual intervention',
    description: 'After one-time setup, InciTeam keeps polling, evaluating, assigning, notifying, and logging the result.',
  },
];

const iosScreenshots = [
  { src: '/marketing/mobile/ios-home.png', label: 'Home', alt: 'InciTeam iOS home screen' },
  { src: '/marketing/mobile/ios-roster.png', label: 'Roster', alt: 'InciTeam iOS roster screen' },
  { src: '/marketing/mobile/ios-schedule.png', label: 'Schedule', alt: 'InciTeam iOS schedule screen' },
  { src: '/marketing/mobile/ios-team-members.png', label: 'Team Members', alt: 'InciTeam iOS team members screen' },
  { src: '/marketing/mobile/ios-ci.png', label: 'Configuration Items', alt: 'InciTeam iOS configuration items screen' },
  { src: '/marketing/mobile/ios-ci-mapping.png', label: 'CI Mapping', alt: 'InciTeam iOS CI user mapping screen' },
  { src: '/marketing/mobile/ios-logs.png', label: 'Logs', alt: 'InciTeam iOS ServiceNow logs screen' },
  { src: '/marketing/mobile/ios-account.png', label: 'Account', alt: 'InciTeam iOS account screen' },
  { src: '/marketing/mobile/ios-summary.png', label: 'Summary', alt: 'InciTeam iOS operations summary screen' },
];

const androidScreenshots = [
  { src: '/marketing/mobile/android-home.png', label: 'Home', alt: 'InciTeam Android home screen' },
  { src: '/marketing/mobile/android-roster.png', label: 'Roster', alt: 'InciTeam Android roster screen' },
  { src: '/marketing/mobile/android-schedule.png', label: 'Schedule', alt: 'InciTeam Android schedule screen' },
  { src: '/marketing/mobile/android-team-members.png', label: 'Team Members', alt: 'InciTeam Android team members screen' },
  { src: '/marketing/mobile/android-ci.png', label: 'Configuration Items', alt: 'InciTeam Android configuration items screen' },
  { src: '/marketing/mobile/android-ci-mapping.png', label: 'CI Mapping', alt: 'InciTeam Android CI user mapping screen' },
  { src: '/marketing/mobile/android-logs.png', label: 'Logs', alt: 'InciTeam Android ServiceNow logs screen' },
  { src: '/marketing/mobile/android-account.png', label: 'Account', alt: 'InciTeam Android account screen' },
  { src: '/marketing/mobile/android-summary.png', label: 'Summary', alt: 'InciTeam Android operations summary screen' },
];

const mobileShowcases = [
  {
    id: 'ios',
    eyebrow: 'iOS Companion',
    title: 'A native iPhone view for live incident operations.',
    description:
      'Roster, schedule, CI ownership, logs, summary health, account controls, and APNs assignment alerts stay close to managers and responders.',
    screenshots: iosScreenshots,
  },
  {
    id: 'android',
    eyebrow: 'Android Companion',
    title: 'The same post-setup workflow on Android.',
    description:
      'Android teams get the home hub, roster windows, schedules, team members, CI records, mapping order, logs, summary health, and FCM alerts.',
    screenshots: androidScreenshots,
  },
];

export default function MarketingHomePage() {
  const currentUser = getCurrentUser();
  const primaryPath = currentUser ? getDefaultRouteForUser(currentUser) : '/signup';
  const primaryLabel = currentUser ? 'Open App' : 'Create Organization';
  const authLinkProps = currentUser ? {} : { target: '_blank', rel: 'noreferrer' };

  return (
    <div className="marketing-page">
      <header className="marketing-nav">
        <Link className="marketing-nav__brand" to="/">
          <span className="marketing-nav__mark">IT</span>
          <span>
            <span className="marketing-nav__name">InciTeam</span>
            <span className="marketing-nav__line">Incident and team management platform for ServiceNow</span>
          </span>
        </Link>
        <nav className="marketing-nav__links" aria-label="Marketing navigation">
          <a href="#problem">Problem</a>
          <a href="#workflow">Workflow</a>
          <a href="#platform">Platform</a>
          <a href="#mobile">Mobile</a>
          <Link to="/privacy">Privacy</Link>
        </nav>
        <div className="marketing-nav__actions">
          <Link className="marketing-link-button" to="/signin" {...authLinkProps}>
            Sign In
          </Link>
          <Link className="marketing-nav__cta" to={primaryPath} {...authLinkProps}>
            {primaryLabel}
          </Link>
        </div>
      </header>

      <main>
        <section className="marketing-hero">
          <div className="marketing-hero__inner">
            <div className="marketing-hero__copy">
              <p className="marketing-kicker">Incident assignment without the manual bottleneck</p>
              <h1>Assign ServiceNow incidents to the right person at the right time.</h1>
              <p className="marketing-hero__lead">
                InciTeam helps teams use ownership, schedules, leave, breaks, workload, logs, and
                mobile alerts to make incident assignment clear, fast, and consistent.
              </p>
            </div>
          </div>
        </section>

        <section className="marketing-metrics" aria-label="InciTeam platform snapshot">
          <div>
            <strong>One-time setup</strong>
            <span>Create organizations, teams, schedules, CI owners, and notification rules</span>
          </div>
          <div>
            <strong>5 min polling</strong>
            <span>Automatic ServiceNow checks, plus manual polling for authorized users</span>
          </div>
          <div>
            <strong>Fair routing</strong>
            <span>Round-robin assignment with leave, break, and critical workload awareness</span>
          </div>
          <div>
            <strong>Alerts</strong>
            <span>Slack, email, and mobile notifications for assignment activity</span>
          </div>
        </section>

        <section className="marketing-section marketing-problem" id="problem">
          <div className="marketing-section__intro">
            <p className="marketing-kicker">The Problem</p>
            <h2>Manual triage works until the moment it matters most.</h2>
            <p>
              A single coordinator can carry the queue on a normal day. During outages, shift changes,
              leave windows, and critical incidents, that same workflow becomes fragile.
            </p>
          </div>
          <div className="marketing-problem__grid">
            {manualTriageProblems.map((problem) => (
              <article className="marketing-problem-card" key={problem.label}>
                <strong>{problem.metric}</strong>
                <span>{problem.label}</span>
                <p>{problem.detail}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="marketing-section marketing-workflow" id="workflow">
          <div className="marketing-section__intro">
            <p className="marketing-kicker">How It Works</p>
            <h2>Set it up once. Let InciTeam handle the assignment flow.</h2>
            <p>
              The product connects team structure, ServiceNow incidents, availability, fair routing,
              and notifications into one continuous workflow.
            </p>
          </div>
          <div className="marketing-workflow__grid">
            {workflowSteps.map((step) => (
              <article className="marketing-workflow-step" key={step.step}>
                <span>{step.step}</span>
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="marketing-section marketing-solution" id="platform">
          <div className="marketing-section__intro marketing-section__intro--center">
            <p className="marketing-kicker">The InciTeam Platform</p>
            <h2>One operating layer for setup, assignment, evidence, and response.</h2>
            <p>
              Keep the web app as the command center. Give responders focused mobile views. Let ServiceNow
              assignment decisions follow the actual team model.
            </p>
          </div>
          <div className="marketing-platform-grid">
            {platformHighlights.map((highlight) => (
              <article className="marketing-platform-card" key={highlight.title}>
                <span>{highlight.eyebrow}</span>
                <h3>{highlight.title}</h3>
                <p>{highlight.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="marketing-section marketing-use-cases">
          <div className="marketing-section__intro">
            <p className="marketing-kicker">Use Cases</p>
            <h2>Built for the people who feel incident pressure first.</h2>
          </div>
          <div className="marketing-use-case-grid">
            {useCases.map((useCase) => (
              <article className="marketing-use-case" key={useCase.role}>
                <span>{useCase.role}</span>
                <h3>{useCase.title}</h3>
                <p>{useCase.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="marketing-section marketing-mobile" id="mobile">
          <div className="marketing-section__intro marketing-section__intro--center">
            <p className="marketing-kicker">Native Mobile Companions</p>
            <h2>Operations context that fits in your hand.</h2>
            <p>
              The mobile apps are intentionally post-setup companions: clean, fast views for roster,
              schedule, CI ownership, logs, summary health, account controls, and assignment alerts.
            </p>
          </div>

          <div className="marketing-mobile-platforms">
            {mobileShowcases.map((platform) => (
              <article className="marketing-mobile-platform" key={platform.id}>
                <div className="marketing-mobile-platform__copy">
                  <span>{platform.eyebrow}</span>
                  <h3>{platform.title}</h3>
                  <p>{platform.description}</p>
                </div>
                <div className="marketing-screenwall" aria-label={`InciTeam ${platform.eyebrow} screenshots`}>
                  <figure className="marketing-screenwall__hero">
                    <div className="marketing-screenwall__device">
                      <img
                        src={platform.screenshots[0].src}
                        alt={platform.screenshots[0].alt}
                        loading="lazy"
                      />
                    </div>
                    <figcaption>{platform.screenshots[0].label}</figcaption>
                  </figure>
                  <div className="marketing-screenwall__tiles">
                    {platform.screenshots.slice(1).map((screenshot) => (
                      <figure className="marketing-screen-tile" key={screenshot.src}>
                        <img src={screenshot.src} alt={screenshot.alt} loading="lazy" />
                        <figcaption>{screenshot.label}</figcaption>
                      </figure>
                    ))}
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="marketing-cta">
          <div>
            <p className="marketing-kicker">Ready To Launch</p>
            <h2>If your organization works with ServiceNow, try InciTeam and see how calm assignment can feel.</h2>
          </div>
          <div className="marketing-cta__actions">
            <Link className="btn btn-primary btn-lg" to={primaryPath} {...authLinkProps}>
              {primaryLabel}
            </Link>
            <Link className="btn btn-outline-primary btn-lg" to="/signin" {...authLinkProps}>
              Sign In
            </Link>
          </div>
        </section>
      </main>

      <footer className="legal-footer legal-footer--marketing">
        <div className="legal-footer__brand">InciTeam</div>
        <div>
          <Link to="/privacy">Privacy Policy</Link>
        </div>
        <div>Copyright © 2026 Parth Sarvan. All Rights Reserved.</div>
        <div>U.S. Trademark Application Serial No. 99808275.</div>
        <div>U.S. Copyright Case No. 1-15157770821 pending.</div>
      </footer>
    </div>
  );
}
